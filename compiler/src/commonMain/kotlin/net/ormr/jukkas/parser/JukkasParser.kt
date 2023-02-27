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
import net.ormr.jukkas.Path
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenStream
import net.ormr.jukkas.lexer.TokenType
import net.ormr.jukkas.lexer.TokenType.*
import net.ormr.jukkas.parser.parselets.prefix.PrefixParselet
import net.ormr.jukkas.parser.parselets.prefix.StringParselet
import net.ormr.jukkas.utils.identifierName

class JukkasParser private constructor(tokens: TokenStream) : Parser(tokens) {
    private val tables = ArrayDeque<Table>()
    val table: Table
        get() = tables.firstOrNull() ?: error("No tables found")

    fun newTable(): Table = Table(tables.firstOrNull())

    fun pushTable(table: Table = newTable()) {
        tables.addFirst(table)
    }

    fun popTable() {
        tables.removeFirst()
    }

    inline fun <T> newBlock(table: Table = newTable(), block: () -> T): T {
        pushTable(table)
        return try {
            block()
        } finally {
            popTable()
        }
    }

    fun parseCompilationUnit(): CompilationUnit = newBlock(Table()) {
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
        return CompilationUnit(source, position, imports, children, table)
    }

    fun consumeIdentifier(): Token = consume(IDENTIFIERS, "identifier")

    /**
     * Returns a list of identifiers in a potentially qualified name.
     *
     * Does not error if the given identifier is a plain identifier, but rather just returns a list of size `1`.
     *
     * Note that the separators *(`/`)* are not included in the list, *only* the identifiers are.
     */
    // TODO: handle nested classes identifiers
    fun parseQualifiedIdentifier(): List<Token> = buildList {
        add(consumeIdentifier())
        while (match(SLASH)) {
            add(consumeIdentifier())
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
        val name = consumeIdentifier()
        val alias = when {
            match(AS) -> consumeIdentifier()
            else -> null
        }
        val position = alias?.let { createSpan(name, it) } ?: name.findPosition()
        return ImportEntry(name.identifierName, alias?.identifierName) withPosition position
    }

    private fun parseTopLevel(): TopLevel? = withSynchronization(
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

    private fun parseBasicTypeName(): BasicTypeName {
        val identifier = consumeIdentifier()
        return BasicTypeName(identifier.identifierName) withPosition identifier
    }

    fun parseTypeDeclaration(separator: TokenType = COLON): DefinedTypeName {
        consume(separator)
        return parseBasicTypeName()
    }

    inline fun parseOptionalTypeDeclaration(
        separator: TokenType = COLON,
        defaultPosition: () -> Positionable,
    ): TypeName = when {
        check(separator) -> parseTypeDeclaration(separator)
        else -> UndefinedTypeName() withPosition defaultPosition()
    }

    private fun parseFunction(): FunctionDeclaration = newBlock {
        val keyword = consume(FUN)
        val name = consumeIdentifier().identifierName
        consume(LEFT_PAREN)
        val arguments = parseArguments(COMMA, RIGHT_PAREN, ::parseDefaultArgument)
        val argEnd = consume(RIGHT_PAREN)
        val returnType = parseOptionalTypeDeclaration(ARROW) { createSpan(keyword, argEnd) }
        val returnTypePosition = (returnType as? DefinedTypeName)
        val body = when {
            match(EQUAL) -> {
                // TODO: give warning for structures like 'fun() = return;' ?
                val equal = previous()
                val expr = parseExpressionStatement()
                Block(newTable(), listOf(expr)) withPosition createSpan(equal, expr)
            }
            match(LEFT_BRACE) -> parseBlock(RIGHT_BRACE)
            // TODO: verify that the function is actually abstract if no body exists in the verifier
            //       and also verify that only class level functions are marked abstract and that stuff
            else -> null
        }
        val position = createSpan(keyword, body ?: returnTypePosition ?: argEnd)
        return FunctionDeclaration(name, arguments, body, returnType, table) withPosition position
    }

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
        val name = consumeIdentifier()
        val type = parseTypeDeclaration()
        return BasicArgument(name.identifierName, type) withPosition createSpan(name, type)
    }

    fun parseDefaultArgument(): NamedArgument {
        val name = consumeIdentifier()
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

    private fun parseVariable(): LocalVariable = TODO("parseVariable")

    fun parseStatement(): AbstractStatement = when {
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

    fun parseBlock(blockEnd: TokenType): Block = newBlock {
        val start = previous()
        val statements = buildList {
            while (!check(blockEnd) && hasMore()) {
                val statement = withSynchronization({ check<BlockSynch>() }, { null }, ::parseStatement) ?: continue
                add(statement)
            }
        }
        val end = consume(blockEnd)
        return Block(table, statements) withPosition createSpan(start, end)
    }

    inline infix fun <R> with(block: JukkasParser.() -> R): R = run(block)

    companion object {
        internal val IDENTIFIERS = TokenType.setOf<IdentifierLike>()
        internal val PROPERTIES = hashSetOf(VAL, VAR)

        internal fun createTokenStream(source: Source): TokenStream = TokenStream.from(source)

        @PublishedApi
        internal fun of(source: Source): JukkasParser {
            return JukkasParser(createTokenStream(source))
        }

        inline fun <T> parse(source: Source, crossinline action: JukkasParser.() -> T): JukkasResult<T> {
            val parser = of(source)
            return try {
                parser.reporter.toResult { action.invoke(parser) }
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