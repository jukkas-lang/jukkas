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

class ImportEntry(val name: String, val alias: String? = null) : ChildNode() {
    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is ImportEntry && name == other.name && alias == other.alias

    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitImportEntry(this)

    override fun toString(): String = alias?.let { "(as $it $name)" } ?: name
}