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

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.ast.DoubleLiteral
import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.FloatLiteral
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.parser.JukkasParser

object DoubleLiteralParselet : PrefixParselet {
    private const val FAILED_CONVERT = "The value is out of range"
    private val DECIMAL32 = DecimalMode(decimalPrecision = 7, roundingMode = RoundingMode.ROUND_HALF_TO_EVEN)
    private val DECIMAL64 = DecimalMode(decimalPrecision = 16, roundingMode = RoundingMode.ROUND_HALF_TO_EVEN)

    override fun parse(parser: JukkasParser, token: Token): Expression = parser with {
        val isFloat = token.text.hasPostfix("f", "F")
        val text = token.text.replace("_", "").let { if (isFloat) it.dropLast(1) else it }
        val bigDecimal = parseNumber(token, text, if (isFloat) DECIMAL32 else DECIMAL64)
        when {
            isFloat -> {
                val value = bigDecimal.convert(BigDecimal::floatValue) { token syntaxError FAILED_CONVERT }
                FloatLiteral(value)
            }
            else -> {
                val value = bigDecimal.convert(BigDecimal::doubleValue) { token syntaxError FAILED_CONVERT }
                DoubleLiteral(value)
            }
        } withPosition token
    }

    private inline fun <reified T> BigDecimal.convert(
        converter: (BigDecimal, Boolean) -> T,
        fallback: BigDecimal.() -> T,
    ): T = try {
        converter(this, false)
    } catch (_: ArithmeticException) {
        fallback()
    }

    private fun String.hasPostfix(a: String, b: String): Boolean = endsWith(a) || endsWith(b)

    private fun JukkasParser.parseNumber(
        position: Positionable,
        text: String,
        mode: DecimalMode,
    ): BigDecimal = try {
        text.toBigDecimal(decimalMode = mode)
    } catch (e: NumberFormatException) {
        // TODO: if this fails then that means there's a problem with our lexer, so should this
        //       be like an internal error or something instead?
        position syntaxError "Invalid double: ${e.message}"
    }
}