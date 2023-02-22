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
import net.ormr.jukkas.ast.DefinedTypeName
import net.ormr.jukkas.ast.LambdaDeclaration
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType.*
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.parser.JukkasParser.Companion.IDENTIFIERS

object AnonymousFunctionParselet : PrefixParselet {
    override fun parse(parser: JukkasParser, token: Token): LambdaDeclaration = parser with {
        newBlock {
            val name = consumeIfMatch(IDENTIFIERS, "identifier")
            name?.syntaxError("Anonymous functions with names are prohibited")
            consume(LEFT_PAREN)
            val arguments = parseArguments(COMMA, RIGHT_PAREN, ::parseDefaultArgument)
            val argEnd = consume(RIGHT_PAREN)
            val returnType = parseOptionalTypeDeclaration(ARROW) { createSpan(token, argEnd) }
            val returnTypePosition = (returnType as? DefinedTypeName)
            val body = when {
                match(EQUAL) -> {
                    // TODO: give warning for structures like 'fun() = return;' ?
                    val equal = previous()
                    val expr = parseExpressionStatement()
                    Block(newTable(), listOf(expr)) withPosition createSpan(equal, expr)
                }
                match(LEFT_BRACE) -> parseBlock(RIGHT_BRACE)
                else -> createSpan(token, returnTypePosition ?: argEnd) syntaxError "Function must have a body"
            }
            LambdaDeclaration(arguments, body, returnType, table) withPosition createSpan(token, body)
        }
    }
}