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
import io.kotest.property.arbitrary.numericDouble
import io.kotest.property.checkAll
import net.ormr.jukkas.double
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess
import net.ormr.jukkas.stringify

class DoubleLiteralParsingTest : FunSpec({
    test("Parse positive double form 1") {
        checkAll(Arb.numericDouble(min = 0.1)) { double ->
            parseExpression(stringify(double)) shouldBeSuccess { expr, _ ->
                expr shouldBeStructurallyEquivalentTo double(double)
            }
        }
    }
})