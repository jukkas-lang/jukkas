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
import net.ormr.jukkas.type.UnknownType

class MemberAccessOperation(
    left: Expression,
    value: Expression,
    val isSafe: Boolean,
) : Expression() {
    var left: Expression by child(left)
    var value: Expression by child(value)
    override var type: Type = UnknownType

    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitCall(this)

    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is MemberAccessOperation &&
                isSafe == other.isSafe &&
                left.isStructurallyEquivalent(other.left) &&
                value.isStructurallyEquivalent(other.value)
}