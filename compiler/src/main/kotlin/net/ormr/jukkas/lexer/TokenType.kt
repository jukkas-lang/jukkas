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

import net.ormr.jukkas.utils.joinWithOr
import net.ormr.krautils.collections.asUnmodifiableList

sealed interface TokenType {
    companion object {
        val values: List<TokenType> by lazy {
            AbstractTokenType::class.sealedSubclasses.mapNotNull { it.objectInstance }.asUnmodifiableList()
        }

        inline fun <reified T : TokenType> ofType(): List<T> = values.filterIsInstance<T>()

        inline fun <reified T : TokenType> setOf(): Set<T> = values.filterIsInstanceTo(hashSetOf())
    }

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
    object SYMBOL_LITERAL : AbstractTokenType("symbol literal")
    object MAP_LITERAL_START : AbstractTokenType("#{")
    object TUPLE_LITERAL_START : AbstractTokenType("#(")

    // string literal
    object STRING_START : AbstractTokenType("\"")
    object STRING_END : AbstractTokenType("\"")
    object ESCAPE_SEQUENCE : AbstractTokenType("escape sequence")
    object STRING_CONTENT : AbstractTokenType("string content")
    object STRING_TEMPLATE_START : AbstractTokenType("\\{")
    object STRING_TEMPLATE_END : AbstractTokenType("}")

    // quote
    object QUOTE_START : AbstractTokenType("```")
    object QUOTE_END : AbstractTokenType("```")

    // identifier
    object IDENTIFIER : AbstractTokenType("identifier"), IdentifierLike
    object ESCAPED_IDENTIFIER : AbstractTokenType("escaped identifier"), IdentifierLike
    sealed interface IdentifierLike : TokenType

    // keyword
    object IMPORT : AbstractTokenType("import"), Keyword, TopSynch
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
    sealed interface Keyword : TokenType

    // soft keywords
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
            val charName = Character.getName(char.code) ?: char.code.toString()
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
}