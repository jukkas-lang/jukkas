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

import net.ormr.jukkas.ast.LambdaDeclaration
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType.ARROW
import net.ormr.jukkas.lexer.TokenType.COMMA
import net.ormr.jukkas.lexer.TokenType.RIGHT_BRACE
import net.ormr.jukkas.lexer.TokenType.VERTICAL_LINE
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.type.UnknownType

/**
 * Parses an empty block as a function.
 *
 * For example:
 * ```jukkas
 *  val func = { person: Person, any: Anything ->
 *      // stuff
 *  };
 * ```
 *
 * ```jukkas
 *  val func = { (name, age: personAge, gender: _), _ ->
 *      // stuff
 *  };
 * ```
 */
object FunctionLiteralParselet : PrefixParselet {
    override fun parse(parser: JukkasParser, token: Token): LambdaDeclaration = parser with {
        newBlock {
            // TODO: we're using || to separate arguments for now, remove this at a later point,
            //       will require arbitrary lookahead tho
            val arguments = when {
                check(VERTICAL_LINE) -> {
                    consume()
                    val arguments = parseArguments(COMMA, VERTICAL_LINE, ::parsePatternArgument)
                    consume(VERTICAL_LINE)
                    consume(ARROW)
                    arguments
                }
                else -> {
                    if (check(ARROW)) consume()
                    emptyList()
                }
            }
            // TODO: set the end of the position of this block to its last child
            val body = parseBlock(RIGHT_BRACE)
            val end = previous()
            LambdaDeclaration(arguments, body, UnknownType, table) withPosition createSpan(token, end)
        }
    }
}