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
import net.ormr.jukkas.ast.Import
import net.ormr.jukkas.ast.ImportEntry
import net.ormr.jukkas.ast.StringLiteral
import net.ormr.jukkas.parseImport
import net.ormr.jukkas.shouldBeStructurallyEquivalentTo
import net.ormr.jukkas.shouldBeSuccess

class BasicImportParsingTest : FunSpec({
    test("import Foo") {
        parseImport("import \"foo/bar\" { Foo }") shouldBeSuccess { import, _ ->
            import shouldBeStructurallyEquivalentTo Import(
                listOf(ImportEntry("Foo")),
                StringLiteral("foo/bar"),
            )
        }
    }

    test("import Foo, Bar") {
        parseImport("import \"foo/bar\" { Foo, Bar }") shouldBeSuccess { import, _ ->
            import shouldBeStructurallyEquivalentTo Import(
                listOf(
                    ImportEntry("Foo"),
                    ImportEntry("Bar"),
                ),
                StringLiteral("foo/bar"),
            )
        }
    }
})