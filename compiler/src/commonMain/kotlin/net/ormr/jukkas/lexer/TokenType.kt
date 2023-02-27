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

@file:Suppress("ClassName")

package net.ormr.jukkas.lexer

import net.ormr.jukkas.utils.getCharName
import net.ormr.jukkas.utils.joinWithOr

@Suppress("diktat")
sealed interface TokenType {
    val image: String

    // separators
    object ARROW : AbstractTokenType("->")
    object LEFT_BRACE : AbstractTokenType("{")
    object RIGHT_BRACE : AbstractTokenType("}")
    object LEFT_PAREN : AbstractTokenType("(")
    object RIGHT_PAREN : AbstractTokenType(")")
    object LEFT_BRACKET : AbstractTokenType("[")
    object RIGHT_BRACKET : AbstractTokenType("]")
    object VERTICAL_LINE : AbstractTokenType("|")
    object SEMICOLON : AbstractTokenType(";")
    object COLON : AbstractTokenType(":")
    object COMMA : AbstractTokenType(",")
    object HOOK : AbstractTokenType("?")

    // assignments
    object EQUAL : AbstractTokenType("="), Assignment
    sealed interface Assignment : TokenType

    // operators
    object DOT : AbstractTokenType("."), Call
    object HOOK_DOT : AbstractTokenType("?."), Call
    sealed interface Call : TokenType

    object PLUS : AbstractTokenType("+"), Additive
    object MINUS : AbstractTokenType("-"), Additive
    sealed interface Additive : TokenType

    object STAR : AbstractTokenType("*"), Multiplicative
    object SLASH : AbstractTokenType("/"), Multiplicative
    sealed interface Multiplicative : TokenType

    object EQUAL_EQUAL : AbstractTokenType("=="), Equality
    object BANG_EQUAL : AbstractTokenType("!="), Equality
    sealed interface Equality : TokenType

    // literals
    object INT_LITERAL : AbstractTokenType("int literal")
    object CHAR_LITERAL : AbstractTokenType("char literal")
    object MAP_LITERAL_START : AbstractTokenType("#{")
    object TUPLE_LITERAL_START : AbstractTokenType("#(")

    // string literal
    object STRING_START : AbstractTokenType("\"")
    object STRING_END : AbstractTokenType("\""), StringContentSynch
    object ESCAPE_SEQUENCE : AbstractTokenType("escape sequence"), StringContentSynch
    object STRING_CONTENT : AbstractTokenType("string content"), StringContentSynch
    object STRING_TEMPLATE_START : AbstractTokenType("\\{"), StringContentSynch
    object STRING_TEMPLATE_END : AbstractTokenType("}"), StringContentSynch
    sealed interface StringContentSynch : TokenType

    // quote
    object QUOTE_START : AbstractTokenType("```")
    object QUOTE_END : AbstractTokenType("```")

    // identifier
    object IDENTIFIER : AbstractTokenType("identifier"), IdentifierLike
    object ESCAPED_IDENTIFIER : AbstractTokenType("escaped identifier"), IdentifierLike
    sealed interface IdentifierLike : TokenType

    // keyword
    object FUN : AbstractTokenType("func"), Keyword, TopSynch
    object VAL : AbstractTokenType("val"), Keyword, TopSynch
    object VAR : AbstractTokenType("var"), Keyword, TopSynch
    object TRUE : AbstractTokenType("true"), Keyword
    object FALSE : AbstractTokenType("false"), Keyword
    object RETURN : AbstractTokenType("return"), Keyword, BlockSynch
    object IF : AbstractTokenType("if"), Keyword, BlockSynch
    object ELSE : AbstractTokenType("else"), Keyword
    object AND : AbstractTokenType("and"), Keyword
    object OR : AbstractTokenType("or"), Keyword
    object NOT : AbstractTokenType("not"), Keyword
    object AS : AbstractTokenType("as"), Keyword // TODO: add parsing support as binary operator
    sealed interface Keyword : TokenType

    // soft keywords
    object IMPORT : AbstractTokenType("import"), SoftKeyword, TopSynch
    object GET : AbstractTokenType("get"), SoftKeyword
    object SET : AbstractTokenType("set"), SoftKeyword
    sealed interface SoftKeyword : TokenType, IdentifierLike

    // error
    object UNEXPECTED_CHARACTER : AbstractTokenType("<Unexpected Character>"), ErrorType {
        override fun format(expected: TokenType, token: Token): String = message(expected.image, token)

        override fun formatMulti(expected: Set<TokenType>, expectedName: String?, token: Token): String =
            message(expectedName ?: expected.joinWithOr(), token)

        private fun message(expected: String, token: Token): String {
            // TODO: token.text should always contain a single character with the way we tokenize
            val char = token.text.single()
            val charName = getCharName(char) ?: char.code.toString()
            return "Expected $expected, got character '$char' ($charName)"
        }
    }

    sealed interface ErrorType : TokenType {
        fun format(expected: TokenType, token: Token): String

        fun formatMulti(expected: Set<TokenType>, expectedName: String?, token: Token): String
    }

    // eof
    object END_OF_FILE : AbstractTokenType("<End Of File>")

    sealed class AbstractTokenType(override val image: String) : TokenType {
        override fun toString(): String = image
    }

    sealed interface TopSynch : TokenType, BlockSynch
    sealed interface BlockSynch : TokenType

    companion object {
        // have to do it manually because only jvm supports any amount of proper reflection
        val values: List<TokenType> = listOf(
            ARROW,
            LEFT_BRACE,
            RIGHT_BRACE,
            LEFT_PAREN,
            RIGHT_PAREN,
            LEFT_BRACKET,
            RIGHT_BRACKET,
            VERTICAL_LINE,
            SEMICOLON,
            COLON,
            COMMA,
            HOOK,
            EQUAL,
            DOT,
            HOOK_DOT,
            PLUS,
            MINUS,
            STAR,
            SLASH,
            EQUAL_EQUAL,
            BANG_EQUAL,
            INT_LITERAL,
            CHAR_LITERAL,
            MAP_LITERAL_START,
            TUPLE_LITERAL_START,
            STRING_START,
            STRING_END,
            ESCAPE_SEQUENCE,
            STRING_CONTENT,
            STRING_TEMPLATE_START,
            STRING_TEMPLATE_END,
            QUOTE_START,
            QUOTE_END,
            IDENTIFIER,
            ESCAPED_IDENTIFIER,
            FUN,
            VAL,
            VAR,
            TRUE,
            FALSE,
            RETURN,
            IF,
            ELSE,
            AND,
            OR,
            NOT,
            AS,
            IMPORT,
            GET,
            SET,
            UNEXPECTED_CHARACTER,
            END_OF_FILE,
        )

        inline fun <reified T : TokenType> ofType(): List<T> = values.filterIsInstance<T>()

        inline fun <reified T : TokenType> setOf(): Set<T> = values.filterIsInstanceTo(hashSetOf())
    }
}