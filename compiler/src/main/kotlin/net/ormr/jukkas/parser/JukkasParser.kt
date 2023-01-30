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
import net.ormr.jukkas.type.TypeName
import net.ormr.jukkas.utils.identifierName
import java.nio.file.Path

class JukkasParser private constructor(tokens: TokenStream) : Parser(tokens) {
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

        fun parseFile(file: Path): JukkasResult<CompilationUnit> =
            parse(Source.File(file), JukkasParser::parseCompilationUnit)
    }

    fun parseCompilationUnit(): CompilationUnit {
        val children = buildList {
            while (hasMore()) {
                add(parseTopLevel() ?: continue)
            }
        }
        val end = consume(END_OF_FILE)
        val position = (children.firstOrNull()?.let { createSpan(it, end) } ?: end).findPosition()
        return CompilationUnit(source, position, children)
    }

    fun parseIdentifier(): Token = consume(IDENTIFIERS, "identifier")

    /**
     * Returns a list of identifiers in a potentially qualified name.
     *
     * Does not error if the given identifier is a plain identifier, but rather just returns a list of size `1`.
     *
     * Note that the separators *(`.`)* are not included in the list, *only* the identifiers are.
     */
    fun parseQualifiedIdentifier(): List<Token> = buildList {
        add(parseIdentifier())
        while (match(DOT)) {
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

    private fun parseImports(): List<Import> = TODO()

    private fun parseTopLevel(): Statement? = withSynchronization(
        { check<TopSynch>() },
        { null },
    ) {
        when {
            check(FUN) -> parseFunction()
            check(PROPERTIES) -> parseProperty()
            else -> current() syntaxError "Expected a top level declaration"
        }
    }

    fun parseTypeName(): TypeName {
        val identifiers = parseQualifiedIdentifier()
        val position = when (identifiers.size) {
            1 -> identifiers.first()
            else -> createSpan(identifiers.first(), identifiers.last())
        }.findPosition()
        return TypeName(position, identifiers.joinToString(separator = "."))
    }

    fun parseTypeDeclaration(): TypeName {
        consume(COLON)
        return parseTypeName()
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
        return InvocationArgument(name?.identifierName, value) withPosition position
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
}