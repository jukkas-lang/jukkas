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

package net.ormr.jukkas.parser.parselets.prefix

import net.ormr.jukkas.ast.Block
import net.ormr.jukkas.ast.Function
import net.ormr.jukkas.ast.Table
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType.COMMA
import net.ormr.jukkas.lexer.TokenType.EQUAL
import net.ormr.jukkas.lexer.TokenType.LEFT_BRACE
import net.ormr.jukkas.lexer.TokenType.LEFT_PAREN
import net.ormr.jukkas.lexer.TokenType.RIGHT_BRACE
import net.ormr.jukkas.lexer.TokenType.RIGHT_PAREN
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.parser.JukkasParser.Companion.IDENTIFIERS
import net.ormr.jukkas.utils.identifierName

object FunctionParselet : PrefixParselet {
    override fun parse(parser: JukkasParser, token: Token): Function = parser with {
        // TODO: give error/warning for usage like 'val thing = fun namedFun()'
        val name = consumeIfMatch(IDENTIFIERS, "identifier")?.identifierName
        consume(LEFT_PAREN)
        val arguments = parseArguments(COMMA, RIGHT_PAREN, ::parseDefaultArgument)
        val argEnd = consume(RIGHT_PAREN)
        val type = parseTypeDeclaration()
        // TODO: type parsing
        val body = when {
            match(EQUAL) -> {
                // TODO: give warning for structures like 'fun() = return;' ?
                val equal = previous()
                val expr = parseExpressionStatement()
                // TODO: proper table stacks
                Block(Table(), listOf(expr)) withPosition createSpan(equal, expr)
            }
            match(LEFT_BRACE) -> parseBlock(RIGHT_BRACE)
            // TODO: verify that the function is actually abstract if no body exists in the verifier
            else -> null
        }
        Function(name, arguments, body, type) withPosition createSpan(token, body ?: argEnd)
    }
}