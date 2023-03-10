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

package net.ormr.jukkas.ast

import net.ormr.jukkas.StructurallyComparable

class ImportEntry(val name: String, val alias: String? = null) : ChildNode() {
    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is ImportEntry && name == other.name && alias == other.alias

    override fun toString(): String = "ImportEntry(name='$name', alias=$alias)"

    operator fun component1(): String = name

    operator fun component2(): String? = alias
}