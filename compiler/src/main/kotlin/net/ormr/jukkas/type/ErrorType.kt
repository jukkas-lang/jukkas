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

package net.ormr.jukkas.type

import net.ormr.jukkas.type.member.TypeMember

/**
 * Represents a type that has been created when a type error was encountered.
 *
 * If a type is an instance of [ErrorType] that means that the AST tree is in a corrupt state and should no longer
 * be worked with.
 */
class ErrorType(val description: String) : ResolvedTypeOrError {
    override val superType: ResolvedType?
        get() = null

    override val interfaces: List<ResolvedType>
        get() = emptyList()

    override val internalName: Nothing
        get() = error("ErrorType: $description")

    override val packageName: Nothing
        get() = error("ErrorType: $description")

    override val simpleName: Nothing
        get() = error("ErrorType: $description")

    override val members: List<TypeMember>
        get() = emptyList()

    override val declaredMembers: List<TypeMember>
        get() = emptyList()

    override fun findMethod(name: String, types: List<ResolvedTypeOrError>): TypeMember.Method? = null

    override fun findConstructor(types: List<ResolvedTypeOrError>): TypeMember.Constructor? = null

    override fun findField(name: String): TypeMember.Field? = null

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = false

    override fun compareCompatibility(other: ResolvedTypeOrError): Int = 0

    override fun isSameType(other: ResolvedTypeOrError): Boolean = false

    override fun toString(): String = "ErrorType[$description]"
}