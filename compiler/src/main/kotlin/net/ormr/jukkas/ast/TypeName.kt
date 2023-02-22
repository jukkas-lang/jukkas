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
import net.ormr.jukkas.type.TypeOrError

sealed class TypeName : ChildNode() {
    var resolvedType: TypeOrError? = null

    abstract fun asString(): String
}

class UndefinedTypeName : TypeName() {
    override fun asString(): String = "<undefined>"

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean = other is UndefinedTypeName
}

sealed class DefinedTypeName : TypeName()

class BasicTypeName(val name: String) : DefinedTypeName() {
    override fun asString(): String = name

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is BasicTypeName && name == other.name
}

class UnionTypeName(left: DefinedTypeName, right: DefinedTypeName) : DefinedTypeName() {
    var left: DefinedTypeName by child(left)
    var right: DefinedTypeName by child(right)

    override fun asString(): String = "${left.asString()} | ${right.asString()}"

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is UnionTypeName &&
            left.isStructurallyEquivalent(other.left) &&
            right.isStructurallyEquivalent(other.right)
}

class IntersectionTypeName(left: DefinedTypeName, right: DefinedTypeName) : DefinedTypeName() {
    var left: DefinedTypeName by child(left)
    var right: DefinedTypeName by child(right)

    override fun asString(): String = "${left.asString()} & ${right.asString()}"

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is UnionTypeName &&
            left.isStructurallyEquivalent(other.left) &&
            right.isStructurallyEquivalent(other.right)
}