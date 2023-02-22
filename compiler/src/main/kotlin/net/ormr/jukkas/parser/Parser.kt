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

import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenStream
import net.ormr.jukkas.lexer.TokenType
import net.ormr.jukkas.lexer.TokenType.SEMICOLON
import net.ormr.jukkas.reporter.MessageReporter
import net.ormr.jukkas.reporter.MessageType
import net.ormr.jukkas.utils.joinWithOr

@Suppress("UnnecessaryAbstractClass")
abstract class Parser(private val tokens: TokenStream) {
    private val buffer = mutableListOf<Token>()
    private val consumed = ArrayDeque<Token>()
    val reporter: MessageReporter = MessageReporter()
    val source: Source
        get() = tokens.source

    /**
     * Returns `true` if [current] is one of [expected], otherwise `false`.
     */
    fun check(vararg expected: TokenType): Boolean = current().type in expected

    /**
     * Returns `true` if [current] is one of [expected], otherwise `false`.
     */
    fun check(expected: Set<TokenType>): Boolean = current().type in expected

    /**
     * Returns `true` if [current] is [expected], otherwise `false`.
     */
    fun check(expected: TokenType): Boolean = current().type == expected

    /**
     * Returns `true` if [current] is one of [T] type, otherwise `false`.
     */
    @JvmName("checkType")
    inline fun <reified T : TokenType> check(): Boolean = current().type is T

    /**
     * Returns `true` if [current] is in [expected] and [consume]s `current`, otherwise `false`.
     */
    fun match(expected: Set<TokenType>): Boolean {
        val token = current()

        if (token.type !in expected) {
            return false
        }

        consume()
        return true
    }

    /**
     * Returns `true` if [current] is [expected] and [consume]s `current`, otherwise `false`.
     */
    fun match(expected: TokenType): Boolean {
        val token = current()

        if (token.type != expected) {
            return false
        }

        consume()
        return true
    }

    fun consumeIfMatch(expected: TokenType): Token? = if (check(expected)) consume() else null

    fun consumeIfMatch(expected: Set<TokenType>, name: String? = null): Token? =
        if (check(expected)) consume(expected, name) else null

    fun consume(expected: TokenType): Token {
        val token = current()

        if (token.type != expected) {
            when (token.type) {
                is TokenType.ErrorType -> token syntaxError token.type.format(expected, token)
                else -> token syntaxError "Expected $expected but got ${token.type}"
            }
        }

        return consume()
    }

    fun consume(expected: Set<TokenType>, name: String? = null): Token {
        val token = current()

        if (token.type !in expected) {
            when (token.type) {
                is TokenType.ErrorType -> token syntaxError token.type.formatMulti(expected, name, token)
                else -> token syntaxError "Expected ${name ?: expected.joinWithOr()} but got ${token.type}"
            }
        }

        return consume()
    }

    fun consume(): Token {
        // make sure we've read the token
        current()

        consumed.addFirst(buffer.removeAt(0))
        return previous()
    }

    fun current(): Token = lookAhead(0)

    fun lookAhead(distance: Int): Token {
        while (distance >= buffer.size) {
            buffer += tokens.next()
        }

        return buffer[distance]
    }

    // TODO: better name

    /**
     * Returns the most recently [consume]d token back to the [buffer] and removes it from [consumed].
     */
    fun unconsume() {
        buffer.add(0, consumed.removeFirst())
    }

    fun previous(@Suppress("IDENTIFIER_LENGTH") n: Int = 0): Token =
        consumed.getOrElse(n) { throw IllegalArgumentException("No previous token found at index $n") }

    fun isAtEnd(): Boolean = !hasMore()

    fun hasMore(): Boolean = tokens.hasNext() && !check(TokenType.END_OF_FILE)

    /**
     * Reports a syntax error at the position of `this`.
     *
     * This does *not* throw a [JukkasParseException], unlike [syntaxError] / [error].
     */
    fun Positionable.reportSyntaxError(message: String) {
        reporter.reportError(source, MessageType.Error.SYNTAX, this, message)
    }

    infix fun Positionable.syntaxError(message: String): Nothing = error(MessageType.Error.SYNTAX, this, message)

    fun error(
        type: MessageType.Error,
        position: Positionable,
        message: String,
    ): Nothing {
        val error = reporter.reportError(source, type, position, message)
        throw JukkasParseException(error)
    }

    protected inline fun synchronize(predicate: () -> Boolean) {
        consume()
        while (hasMore()) {
            if (check(SEMICOLON)) break

            if (predicate()) break

            consume()
        }
    }

    protected inline fun <T> withSynchronization(
        predicate: () -> Boolean,
        onFail: () -> T,
        action: () -> T,
    ): T = try {
        action()
    } catch (_: JukkasParseException) {
        synchronize(predicate)
        onFail()
    }
}