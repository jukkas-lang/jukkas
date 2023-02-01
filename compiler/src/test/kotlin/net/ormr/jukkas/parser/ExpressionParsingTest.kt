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
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess

class ExpressionParsingTest : FunSpec({
    test("'false' should parse to BooleanLiteral(false)") {
        parseExpression("false") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BooleanLiteral(false)
        }
    }

    test("'true' should parse to BooleanLiteral(true)") {
        parseExpression("true") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BooleanLiteral(true)
        }
    }


    test("'12345' should parse to IntLiteral(true)") {
        parseExpression("12345") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo IntLiteral(12345)
        }
    }

    test("'foo' should parse to DefinitionReference('foo')") {
        parseExpression("foo") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo DefinitionReference("foo")
        }
    }

    test("'foo(1, bar = 2, 3)' should parse to FunctionInvocation(...)") {
        parseExpression("foo(1, bar = 2, 3)") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo FunctionInvocation(
                DefinitionReference("foo"),
                listOf(
                    InvocationArgument(null, IntLiteral(1)),
                    InvocationArgument("bar", IntLiteral(2)),
                    InvocationArgument(null, IntLiteral(3)),
                ),
            )
        }
    }

    test("'1 + 2' -> (+ 1 2)") {
        parseExpression("1 + 2") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BinaryOperation(
                IntLiteral(1),
                BinaryOperator.PLUS,
                IntLiteral(2),
            )
        }
    }

    test("'1 + 2 + 3' -> (+ (+ 1 2) 3)") {
        parseExpression("1 + 2 + 3") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BinaryOperation(
                BinaryOperation(
                    IntLiteral(1),
                    BinaryOperator.PLUS,
                    IntLiteral(2),
                ),
                BinaryOperator.PLUS,
                IntLiteral(3),
            )
        }
    }

    test("'1 - 2 * 3 / 4' -> (+ 1 (/ (* 2 3) 4)") {
        parseExpression("1 - 2 * 3 / 4") shouldBeSuccess { expr, _ ->
            expr shouldBeStructurallyEquivalentTo BinaryOperation(
                IntLiteral(1),
                BinaryOperator.MINUS,
                BinaryOperation(
                    BinaryOperation(
                        IntLiteral(2),
                        BinaryOperator.MULTIPLICATION,
                        IntLiteral(3),
                    ),
                    BinaryOperator.DIVISION,
                    IntLiteral(4),
                ),
            )
        }
    }
})