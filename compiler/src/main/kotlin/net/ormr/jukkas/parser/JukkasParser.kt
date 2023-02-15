/*
 * Copyright 2023 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ormr.jukkas.parser

import net.ormr.jukkas.JukkasResult
import net.ormr.jukkas.Position
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.ast.Function
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenStream
import net.ormr.jukkas.lexer.TokenType
import net.ormr.jukkas.lexer.TokenType.*
import net.ormr.jukkas.parser.parselets.prefix.FunctionParselet
import net.ormr.jukkas.parser.parselets.prefix.PrefixParselet
import net.ormr.jukkas.parser.parselets.prefix.StringParselet
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeName
import net.ormr.jukkas.type.UnknownType
import net.ormr.jukkas.utils.identifierName
import java.nio.file.Path

class JukkasParser private constructor(tokens: TokenStream) : Parser(tokens) {
    fun parseCompilationUnit(): CompilationUnit {
        val imports = buildList {
            while (hasMore()) {
                if (!check(IMPORT)) break
                add(parseImport() ?: continue)
            }
        }
        val children = buildList {
            while (hasMore()) {
                add(parseTopLevel() ?: continue)
            }
        }
        val end = consume(END_OF_FILE)
        val position = children.firstOrNull()?.let { createSpan(it, end) } ?: end.findPosition()
        return CompilationUnit(source, position, imports, children)
    }

    fun parseIdentifier(): Token = consume(IDENTIFIERS, "identifier")

    /**
     * Returns a list of identifiers in a potentially qualified name.
     *
     * Does not error if the given identifier is a plain identifier, but rather just returns a list of size `1`.
     *
     * Note that the separators *(`/`)* are not included in the list, *only* the identifiers are.
     */
    // TODO: handle nested classes identifiers
    fun parseQualifiedIdentifier(): List<Token> = buildList {
        add(parseIdentifier())
        while (match(SLASH)) {
            add(parseIdentifier())
        }
    }

    /**
     * Returns an [Expression] parsed from the available tokens, or `null` if no [PrefixParselet] could be found for
     * the [current] token.
     *
     * If no `PrefixParselet` could be found, then the consumed `current` token will be [unconsume]d.
     *
     * @see [parseExpression]
     */
    fun parseExpressionOrNull(precedence: Int = 0): Expression? {
        var token = consume()
        val prefix = Grammar.getPrefixParselet(token)
        if (prefix == null) {
            unconsume()
            return null
        }
        var left = prefix.parse(this, token)

        while (precedence < Grammar.getPrecedence(current())) {
            token = consume()
            val infix = Grammar.getInfixParselet(token) ?: (token syntaxError "Unknown infix operator '${token.text}'")
            left = infix.parse(this, left, token)
        }

        return left
    }

    fun parseExpression(precedence: Int = 0): Expression =
        parseExpressionOrNull(precedence) ?: (current() syntaxError "Expecting expression got ${previous().type}")

    fun parseImport(): Import? = withSynchronization(
        { check<TopSynch>() },
        { null },
    ) {
        val import = consume(IMPORT)
        val pathStart = consume(STRING_START)
        val path = StringParselet.parse(this, pathStart)
        consume(LEFT_BRACE)
        val entries = parseArguments(COMMA, RIGHT_BRACE, ::parseImportEntry)
        val end = consume(RIGHT_BRACE)
        // report error here so we can attempt to properly parse the symbol block first
        if (path !is StringLiteral) path syntaxError "Only simple strings are allowed as paths"
        Import(entries, path) withPosition createSpan(import, end)
    }

    private fun parseImportEntry(): ImportEntry {
        // TODO: handle nested classes identifiers
        val name = parseIdentifier()
        val alias = when {
            match(AS) -> parseIdentifier()
            else -> null
        }
        val position = alias?.let { createSpan(name, it) } ?: name.findPosition()
        return ImportEntry(name.identifierName, alias?.identifierName) withPosition position
    }

    private fun parseTopLevel(): Statement? = withSynchronization(
        { check<TopSynch>() },
        { null },
    ) {
        when {
            check(FUN) -> parseFunction()
            check(PROPERTIES) -> parseProperty()
            check(IMPORT) -> current() syntaxError "'import' must be declared before anything else"
            else -> current() syntaxError "Expected a top level declaration"
        }
    }

    fun parseTypeName(): TypeName {
        val identifier = parseIdentifier()
        return TypeName(identifier.findPosition(), identifier.identifierName)
    }

    fun parseTypeDeclaration(): TypeName {
        consume(COLON)
        return parseTypeName()
    }

    fun parseOptionalTypeDeclaration(): Type = when {
        check(COLON) -> parseTypeDeclaration()
        else -> UnknownType
    }

    fun createTypePosition(start: Positionable, type: Type): Position = when (type) {
        is TypeName -> createSpan(start, type)
        else -> start.findPosition()
    }

    private fun parseFunction(): Function = FunctionParselet.parse(this, consume(FUN))

    private fun parseProperty(): Property = TODO()

    inline fun <T> parseArguments(
        separator: TokenType,
        terminator: TokenType,
        parse: () -> T,
    ): List<T> = when {
        check(terminator) -> emptyList()
        else -> buildList {
            add(parse())
            while (match(separator)) {
                // allows for trailing terminators
                if (check(terminator)) break
                add(parse())
            }
        }
    }

    fun parseInvocationArgument(): InvocationArgument {
        val name = consumeIfMatch(IDENTIFIERS, "identifier")
        // TODO: should we use colon instead?
        if (name != null) consume(EQUAL)
        val value = parseExpression()
        val position = name?.let { createSpan(it, value) } ?: value
        return InvocationArgument(value, name?.identifierName) withPosition position
    }

    fun parseBasicArgument(): BasicArgument {
        val name = parseIdentifier()
        val type = parseTypeDeclaration()
        return BasicArgument(name.identifierName, type) withPosition createSpan(name, type)
    }

    fun parseDefaultArgument(): Argument {
        val name = parseIdentifier()
        val identifierName = name.identifierName
        val type = parseTypeDeclaration()
        return when (val default = if (match(EQUAL)) parseExpression() else null) {
            null -> BasicArgument(identifierName, type) withPosition createSpan(name, type)
            else -> DefaultArgument(identifierName, type, default) withPosition createSpan(name, default)
        }
    }

    fun parsePatternArgument(): Argument = when {
        match(LEFT_PAREN) -> {
            val pattern = parsePattern()
            PatternArgument(pattern)
        }
        else -> parseBasicArgument()
    }

    fun parsePattern(): Pattern = TODO("parsePattern")

    private fun parseVariable(): Variable = TODO("parseVariable")

    private fun parseStatement(): Statement = when {
        check(FUN) -> parseFunction()
        check(PROPERTIES) -> parseVariable()
        else -> parseExpressionStatement()
    }

    fun parseExpressionStatement(precedence: Int = 0): ExpressionStatement {
        val expr = parseExpression(precedence)
        val end = consume(SEMICOLON)
        return ExpressionStatement(expr) withPosition createSpan(expr, end)
    }

    fun parseBlockOrExpression(blockStart: TokenType, blockEnd: TokenType): Expression = when {
        match(blockStart) -> parseBlock(blockEnd)
        else -> parseExpression()
    }

    fun parseBlock(blockEnd: TokenType): Block {
        val start = previous()
        val statements = buildList {
            while (!check(blockEnd) && hasMore()) {
                val statement = withSynchronization({ check<BlockSynch>() }, { null }, ::parseStatement) ?: continue
                add(statement)
            }
        }
        val end = consume(blockEnd)
        // TODO: proper table stacks
        return Block(Table(), statements) withPosition createSpan(start, end)
    }

    inline infix fun <R> with(block: JukkasParser.() -> R): R = run(block)

    companion object {
        internal val IDENTIFIERS = TokenType.setOf<IdentifierLike>()
        internal val PROPERTIES = hashSetOf(VAL, VAR)

        @PublishedApi
        internal fun of(source: Source): JukkasParser {
            val tokens = TokenStream.from(source)
            return JukkasParser(tokens)
        }

        inline fun <T> parse(source: Source, crossinline action: JukkasParser.() -> T): JukkasResult<T> {
            val parser = of(source)
            return try {
                parser.reporter.toResult { parser.use(action) }
            } catch (_: JukkasParseException) {
                JukkasResult.Failure(parser.reporter.messages)
            }
        }

        fun parseText(text: String): JukkasResult<CompilationUnit> =
            parse(Source.Text(text), JukkasParser::parseCompilationUnit)

        fun parseFile(file: Path): JukkasResult<CompilationUnit> =
            parse(Source.File(file), JukkasParser::parseCompilationUnit)
    }
}