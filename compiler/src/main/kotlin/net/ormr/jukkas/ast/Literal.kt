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
import net.ormr.jukkas.type.JvmPrimitiveType
import net.ormr.jukkas.type.JvmReferenceType

sealed class Literal : Expression() {
    abstract override val resolvedType: TypeOrError?
}

class SymbolLiteral(val text: String) : Literal() {
    override lateinit var resolvedType: TypeOrError?

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is SymbolLiteral && text == other.text
}

class IntLiteral(val value: Int) : Literal() {
    override val resolvedType: TypeOrError?
        get() = JvmPrimitiveType.INT

    override fun toString(): String = value.toString()

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is IntLiteral && value == other.value && resolvedType.isStructurallyEquivalent(other.resolvedType)
}

class BooleanLiteral(val value: Boolean) : Literal() {
    override val resolvedType: TypeOrError?
        get() = JvmPrimitiveType.BOOLEAN

    override fun toString(): String = value.toString()

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is BooleanLiteral && value == other.value && resolvedType.isStructurallyEquivalent(other.resolvedType)
}

class StringLiteral(val value: String) : Literal() {
    override val resolvedType: TypeOrError?
        get() = JvmReferenceType.STRING

    override fun toString(): String = "\"$value\""

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is StringLiteral && value == other.value && resolvedType.isStructurallyEquivalent(other.resolvedType)
}