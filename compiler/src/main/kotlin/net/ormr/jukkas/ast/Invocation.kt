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
import net.ormr.jukkas.type.member.TypeMember

sealed class Invocation : Expression() {
    override var type: Type = UnknownType

    final override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitInvocation(this)
}

class InfixInvocation(
    left: Expression,
    val name: String,
    right: Expression,
) : Invocation() {
    var left: Expression by child(left)
    var right: Expression by child(right)

    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is InfixInvocation && name == other.name && left.isStructurallyEquivalent(other.left) &&
                right.isStructurallyEquivalent(other.right)

    operator fun component1(): Expression = left

    operator fun component2(): String = name

    operator fun component3(): Expression = right
}

class FunctionInvocation(left: Expression, arguments: List<InvocationArgument>) : Invocation() {
    var left: Expression by child(left)
    val arguments: MutableNodeList<InvocationArgument> = arguments.toMutableNodeList(this)
    var member: TypeMember? = null

    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is FunctionInvocation && left.isStructurallyEquivalent(other.left) &&
                arguments.size == other.arguments.size &&
                (arguments zip other.arguments).all { (first, second) -> first.isStructurallyEquivalent(second) }

    override fun toString(): String = "($left (${arguments.joinToString(separator = " ")}))"

    operator fun component1(): Expression = left

    operator fun component2(): MutableNodeList<InvocationArgument> = arguments
}