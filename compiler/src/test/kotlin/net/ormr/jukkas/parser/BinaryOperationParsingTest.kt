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
import net.ormr.jukkas.ast.BinaryOperation
import net.ormr.jukkas.ast.BinaryOperator.*
import net.ormr.jukkas.ast.IntLiteral
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess

class BinaryOperationParsingTest : FunSpec({
    test("'1 + 2' -> (+ 1 2)") {
        parseExpression("1 + 2") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BinaryOperation(
                IntLiteral(1),
                PLUS,
                IntLiteral(2),
            )
        }
    }

    test("'1 + 2 + 3' -> (+ (+ 1 2) 3)") {
        parseExpression("1 + 2 + 3") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BinaryOperation(
                BinaryOperation(
                    IntLiteral(1),
                    PLUS,
                    IntLiteral(2),
                ),
                PLUS,
                IntLiteral(3),
            )
        }
    }

    test("'1 - 2 * 3 / 4' -> (+ 1 (/ (* 2 3) 4)") {
        parseExpression("1 - 2 * 3 / 4") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BinaryOperation(
                IntLiteral(1),
                MINUS,
                BinaryOperation(
                    BinaryOperation(
                        IntLiteral(2),
                        MULTIPLICATION,
                        IntLiteral(3),
                    ),
                    DIVISION,
                    IntLiteral(4),
                ),
            )
        }
    }
})