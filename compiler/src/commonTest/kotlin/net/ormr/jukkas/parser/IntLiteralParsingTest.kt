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
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import net.ormr.jukkas.int
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess

class IntLiteralParsingTest : FunSpec({
    test("Parse positive decimal int") {
        checkAll(Arb.nonNegativeInt()) { int ->
            parseExpression(int.toString()) shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo int(int)
            }
        }
    }

    test("Parse positive binary int") {
        checkAll(Arb.nonNegativeInt()) { int ->
            parseExpression("0b${int.toString(radix = 2)}") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo int(int)
            }
        }
    }

    test("Parse positive hexadecimal int") {
        checkAll(Arb.nonNegativeInt()) { int ->
            parseExpression("0x${int.toString(radix = 16)}") shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo int(int)
            }
        }
    }
})