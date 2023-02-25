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

import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.Int32Literal
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.parser.JukkasParser

object IntLiteralParselet : PrefixParselet {
    override fun parse(parser: JukkasParser, token: Token): Expression = parser with {
        val value = token.text.toInt()
        Int32Literal(value) withPosition token
    }
}