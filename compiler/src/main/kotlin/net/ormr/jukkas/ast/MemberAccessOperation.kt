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
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.UnknownType
import net.ormr.jukkas.type.member.TypeMember

class MemberAccessOperation(
    left: Expression,
    right: Expression,
    val isSafe: Boolean,
) : Expression(), HasMutableType {
    var left: Expression by child(left)
    var right: Expression by child(right)
    override var type: Type = UnknownType
    var member: TypeMember? = null

    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitMemberAccessOperation(this)

    // TODO: do we want to check for type here?
    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is MemberAccessOperation &&
                isSafe == other.isSafe &&
                left.isStructurallyEquivalent(other.left) &&
                right.isStructurallyEquivalent(other.right) &&
                type.isStructurallyEquivalent(other.type) &&
                member == other.member

    override fun toString(): String =
        "MemberAccessOperation(isSafe=$isSafe, left=$left, right=$right, type=$type, member=$member)"

    operator fun component1(): Expression = left

    operator fun component2(): Expression = right

    operator fun component3(): Type = type
}