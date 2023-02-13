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

import net.ormr.jukkas.type.Type
import net.ormr.jukkas.utils.bothNullOrEquivalent

class Function(
    val name: String?,
    arguments: List<Argument>,
    body: Block?,
    override var type: Type,
) : Expression(), Definition {
    val arguments: MutableNodeList<Argument> = arguments.toMutableNodeList(this)
    var body: Block? by child(body)

    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitFunction(this)

    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is Function &&
                name == other.name &&
                arguments.size == other.arguments.size &&
                (arguments zip other.arguments).all { (first, second) -> first.isStructurallyEquivalent(second) } &&
                bothNullOrEquivalent(body, other.body) { a, b -> a.isStructurallyEquivalent(b) }
}