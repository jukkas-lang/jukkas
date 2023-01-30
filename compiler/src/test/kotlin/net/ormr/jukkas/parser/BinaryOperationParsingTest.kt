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
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.ormr.jukkas.ast.BinaryOperation
import net.ormr.jukkas.ast.BinaryOperator.PLUS
import net.ormr.jukkas.ast.IntLiteral
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeSuccess

class BinaryOperationParsingTest : FunSpec({
    test("'1 + 2' -> (+ 1 2)") {
        parseExpression("1 + 2") shouldBeSuccess { expr, _ ->
            expr.shouldBeInstanceOf<BinaryOperation>()
            val left = expr.left.shouldBeInstanceOf<IntLiteral>()
            left.value shouldBe 1
            expr.operator shouldBe PLUS
            val right = expr.right.shouldBeInstanceOf<IntLiteral>()
            right.value shouldBe 2
        }
    }

    test("'1 + 2 + 3' -> (+ (+ 1 2) 3)") {
        parseExpression("1 + 2 + 3") shouldBeSuccess { expr, _ ->
            expr.shouldBeInstanceOf<BinaryOperation>()
            expr.left.should {
                it.shouldBeInstanceOf<BinaryOperation>()
                val left = it.left.shouldBeInstanceOf<IntLiteral>()
                left.value shouldBe 1
                it.operator shouldBe PLUS
                val right = it.right.shouldBeInstanceOf<IntLiteral>()
                right.value shouldBe 2
            }
            expr.operator shouldBe PLUS
            val right = expr.right.shouldBeInstanceOf<IntLiteral>()
            right.value shouldBe 3
        }
    }
})