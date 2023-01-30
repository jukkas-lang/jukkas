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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.ormr.jukkas.ast.BooleanLiteral
import net.ormr.jukkas.parseExpression
import net.ormr.jukkas.shouldBeSuccess

class BooleanLiteralParsingTest : FunSpec({
    test("'false' should parse to Literal(false)") {
        parseExpression("false") shouldBeSuccess { expr, _ ->
            expr.shouldBeInstanceOf<BooleanLiteral>()
            expr.value shouldBe false
        }
    }

    test("'true' should parse to Literal(true)") {
        parseExpression("true") shouldBeSuccess { expr, _ ->
            expr.shouldBeInstanceOf<BooleanLiteral>()
            expr.value shouldBe true
        }
    }
})