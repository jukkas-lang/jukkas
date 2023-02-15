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

package net.ormr.jukkas.parser.parselets.infix

import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.MemberAccessOperation
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.parser.Precedence

object MemberAccessOperationParselet : InfixParselet {
    override val precedence: Int
        get() = Precedence.POSTFIX

    override fun parse(
        parser: JukkasParser,
        left: Expression,
        token: Token,
    ): MemberAccessOperation = parser with {
        val isSafe = when (token.type) {
            TokenType.DOT -> false
            TokenType.HOOK_DOT -> true
            else -> token syntaxError "Unknown call operator"
        }
        val right = parseExpression(precedence)
        // TODO: createSpan(token, value.findPosition().startPoint.end) or something?
        MemberAccessOperation(left, right, isSafe) withPosition createSpan(token, right)
    }
}