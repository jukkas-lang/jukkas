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
import net.ormr.jukkas.arg
import net.ormr.jukkas.ast.Function
import net.ormr.jukkas.ast.Table
import net.ormr.jukkas.parseStatement
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess
import net.ormr.jukkas.type.UnknownType
import net.ormr.jukkas.typeName

class FunctionParsingTest : FunSpec({
    context("No body functions") {
        context("Explicit return type") {
            test("fun foo() -> Unit") {
                parseStatement("fun foo() -> Unit") shouldBeSuccess { stmt, _ ->
                    stmt shouldBeStructurallyEquivalentTo Function(
                        name = "foo",
                        arguments = emptyList(),
                        body = null,
                        type = typeName("Unit"),
                        table = Table(),
                    )
                }
            }

            test("fun foo(bar: Bar) -> Unit") {
                parseStatement("fun foo(bar: Bar) -> Unit") shouldBeSuccess { stmt, _ ->
                    stmt shouldBeStructurallyEquivalentTo Function(
                        name = "foo",
                        arguments = listOf(
                            arg("bar", typeName("Bar")),
                        ),
                        body = null,
                        type = typeName("Unit"),
                        table = Table(),
                    )
                }
            }
        }

        context("Implicit return type") {
            test("fun foo() -> Unit") {
                parseStatement("fun foo()") shouldBeSuccess { stmt, _ ->
                    stmt shouldBeStructurallyEquivalentTo Function(
                        name = "foo",
                        arguments = emptyList(),
                        body = null,
                        type = UnknownType,
                        table = Table(),
                    )
                }
            }

            test("fun foo(bar: Bar) -> Unit") {
                parseStatement("fun foo(bar: Bar)") shouldBeSuccess { stmt, _ ->
                    stmt shouldBeStructurallyEquivalentTo Function(
                        name = "foo",
                        arguments = listOf(
                            arg("bar", typeName("Bar")),
                        ),
                        body = null,
                        type = UnknownType,
                        table = Table(),
                    )
                }
            }
        }
    }
})