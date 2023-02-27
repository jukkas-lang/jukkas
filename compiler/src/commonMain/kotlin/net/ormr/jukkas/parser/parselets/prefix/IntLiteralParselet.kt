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

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.IntLiteral
import net.ormr.jukkas.ast.LongLiteral
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.parser.JukkasParser

object IntLiteralParselet : PrefixParselet {
    private const val FAILED_CONVERT = "The value is out of range"

    override fun parse(parser: JukkasParser, token: Token): Expression = parser with {
        val isLong = token.text.hasPostfix("l", "L")
        val text = token.text.replace("_", "").let { if (isLong) it.dropLast(1) else it }
        val bigInt = when {
            text.hasPrefix("0x", "0X") -> parseNumber(token, text.drop(2), base = 16)
            text.hasPrefix("0b", "0B") -> parseNumber(token, text.drop(2), base = 2)
            else -> parseNumber(token, text, base = 10)
        }
        when {
            isLong -> {
                val value = bigInt.convert(BigInteger::longValue) { token syntaxError FAILED_CONVERT }
                LongLiteral(value)
            }
            else -> {
                // try to convert to int first
                val value = bigInt.convert(BigInteger::intValue) {
                    // if that fails then convert to long
                    bigInt.convert(BigInteger::longValue) { token syntaxError FAILED_CONVERT }
                }
                // if the literal is meant for an int, then the type checker will take care of reporting
                // a proper error in case the value is inferred to a long because of its size
                when (value) {
                    is Int -> IntLiteral(value)
                    is Long -> LongLiteral(value)
                    // TODO: internalError
                    else -> token syntaxError "Int literal was somehow converted to <$value>"
                }
            }
        } withPosition token
    }

    private inline fun <reified T> BigInteger.convert(
        converter: (BigInteger, Boolean) -> T,
        fallback: BigInteger.() -> T,
    ): T = try {
        converter(this, true)
    } catch (_: ArithmeticException) {
        fallback()
    }

    // because 'startsWith(a, ignoreCase = true)' is relatively slow
    private fun String.hasPrefix(a: String, b: String): Boolean = startsWith(a) || startsWith(b)

    private fun String.hasPostfix(a: String, b: String): Boolean = endsWith(a) || endsWith(b)

    private fun JukkasParser.parseNumber(position: Positionable, text: String, base: Int): BigInteger = try {
        text.toBigInteger(base)
    } catch (e: NumberFormatException) {
        // TODO: if this fails then that means there's a problem with our lexer, so should this
        //       be like an internal error or something instead?
        position syntaxError "Invalid int: ${e.message}"
    }
}