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

import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType
import net.ormr.jukkas.lexer.TokenType.*
import net.ormr.jukkas.lexer.TokenType.Companion.ofType
import net.ormr.jukkas.parser.Precedence.ADDITIVE
import net.ormr.jukkas.parser.Precedence.CONJUNCTION
import net.ormr.jukkas.parser.Precedence.DISJUNCTION
import net.ormr.jukkas.parser.Precedence.EQUALITY
import net.ormr.jukkas.parser.Precedence.MULTIPLICATIVE
import net.ormr.jukkas.parser.parselets.infix.AnonymousFunctionInvocationParselet
import net.ormr.jukkas.parser.parselets.infix.AssignmentParselet
import net.ormr.jukkas.parser.parselets.infix.BinaryOperationParselet
import net.ormr.jukkas.parser.parselets.infix.InfixInvocationParselet
import net.ormr.jukkas.parser.parselets.infix.InfixParselet
import net.ormr.jukkas.parser.parselets.infix.MemberAccessOperationParselet
import net.ormr.jukkas.parser.parselets.prefix.BooleanParselet
import net.ormr.jukkas.parser.parselets.prefix.FunctionLiteralParselet
import net.ormr.jukkas.parser.parselets.prefix.IfParselet
import net.ormr.jukkas.parser.parselets.prefix.IntParselet
import net.ormr.jukkas.parser.parselets.prefix.ParenthesizedExpressionParselet
import net.ormr.jukkas.parser.parselets.prefix.PrefixParselet
import net.ormr.jukkas.parser.parselets.prefix.ReferenceParselet
import net.ormr.jukkas.parser.parselets.prefix.ReturnParselet
import net.ormr.jukkas.parser.parselets.prefix.StringParselet
import net.ormr.jukkas.parser.parselets.prefix.SymbolParselet

internal object Grammar {
    private val prefixParselets = hashMapOf<TokenType, PrefixParselet>()
    private val infixParselets = hashMapOf<TokenType, InfixParselet>()

    init {
        prefix<IdentifierLike>(ReferenceParselet)
        prefix(SYMBOL_LITERAL, SymbolParselet)
        prefix(RETURN, ReturnParselet)
        prefix(STRING_START, StringParselet)
        prefix(FALSE, BooleanParselet)
        prefix(TRUE, BooleanParselet)
        prefix(INT_LITERAL, IntParselet)
        prefix(IF, IfParselet)
        prefix(LEFT_BRACE, FunctionLiteralParselet)
        prefix(LEFT_PAREN, ParenthesizedExpressionParselet)

        infix<Call>(MemberAccessOperationParselet) // 14
        infix(LEFT_PAREN, AnonymousFunctionInvocationParselet) // 14
        infix<Multiplicative>(BinaryOperationParselet(MULTIPLICATIVE)) // 11
        infix<Additive>(BinaryOperationParselet(ADDITIVE)) // 10
        infix<IdentifierLike>(InfixInvocationParselet) // 8
        infix<Equality>(BinaryOperationParselet(EQUALITY)) // 4
        infix(OR, BinaryOperationParselet(DISJUNCTION)) // 2
        infix(AND, BinaryOperationParselet(CONJUNCTION)) // 3
        infix<Assignment>(AssignmentParselet) // 0
    }

    private fun prefix(type: TokenType, parselet: PrefixParselet) {
        prefixParselets[type] = parselet
    }

    private inline fun <reified T : TokenType> prefix(parselet: PrefixParselet) {
        prefix(ofType<T>(), parselet)
    }

    private fun prefix(types: Iterable<TokenType>, parselet: PrefixParselet) {
        for (type in types) prefix(type, parselet)
    }

    private fun infix(type: TokenType, parselet: InfixParselet) {
        infixParselets[type] = parselet
    }

    private inline fun <reified T : TokenType> infix(parselet: InfixParselet) {
        infix(ofType<T>(), parselet)
    }

    private fun infix(types: Iterable<TokenType>, parselet: InfixParselet) {
        for (type in types) infix(type, parselet)
    }

    fun getPrefixParselet(token: Token): PrefixParselet? = prefixParselets[token.type]

    fun getInfixParselet(token: Token): InfixParselet? = infixParselets[token.type]

    fun getPrecedence(token: Token): Int = infixParselets[token.type]?.precedence ?: 0
}