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

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import net.ormr.jukkas.long
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess

class LongLiteralParsingTest : FunSpec({
    test("Parse positive decimal long with postfix") {
        checkAll(Arb.long(min = 0)) { long ->
            parseExpression("${long}L") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo long(long)
            }
        }
    }

    test("Parse positive binary long with postfix") {
        checkAll(Arb.long(min = 0)) { long ->
            parseExpression("0b${long.toString(radix = 2)}L") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo long(long)
            }
        }
    }

    test("Parse positive hexadecimal long with postfix") {
        checkAll(Arb.long(min = 0)) { long ->
            parseExpression("0x${long.toString(radix = 16)}L") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo long(long)
            }
        }
    }

    // no postfix
    test("Parse positive decimal long without postfix") {
        checkAll(Arb.long(min = Int.MAX_VALUE.toLong() + 1)) { long ->
            parseExpression(long.toString()) shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo long(long)
            }
        }
    }

    test("Parse positive binary long without postfix") {
        checkAll(Arb.long(min = Int.MAX_VALUE.toLong() + 1)) { long ->
            parseExpression("0b${long.toString(radix = 2)}") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo long(long)
            }
        }
    }

    test("Parse positive hexadecimal long without postfix") {
        checkAll(Arb.long(min = Int.MAX_VALUE.toLong() + 1)) { long ->
            parseExpression("0x${long.toString(radix = 16)}") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo long(long)
            }
        }
    }
})