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

package net.ormr.jukkas.utils

import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType

internal val Token.identifierName: String
    get() = when (type) {
        is TokenType.IdentifierLike -> when (type) {
            TokenType.ESCAPED_IDENTIFIER -> text.drop(1)
            is TokenType.SoftKeyword, TokenType.IDENTIFIER -> text
        }
        else -> throw IllegalArgumentException("Can't get identifierName for non identifier token type: $this")
    }