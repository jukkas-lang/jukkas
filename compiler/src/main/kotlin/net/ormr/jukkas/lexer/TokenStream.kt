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

package net.ormr.jukkas.lexer

import net.ormr.jukkas.Point
import net.ormr.jukkas.Source
import java.io.Closeable

class TokenStream private constructor(private val lexer: Lexer, val source: Source) : Iterator<Token>, Closeable {
    private var previous: Token? = null

    private val eof: Token by lazy {
        val point = previous?.point ?: Point(0, 0, 0..0)
        Token(TokenType.END_OF_FILE, "<eof>", point)
    }

    override fun hasNext(): Boolean = !lexer.isAtEnd

    override fun next(): Token = lexer.advance()?.also { previous = it } ?: eof

    override fun close() {
        lexer.close()
    }

    companion object {
        fun from(source: Source): TokenStream = TokenStream(Lexer(source.reader()), source)
    }
}