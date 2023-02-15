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
 * Represents a [ResolvedType] or an [ErrorType].
 */
sealed interface ResolvedTypeOrError : Type {
    val superType: ResolvedType?

    val interfaces: List<ResolvedType>

    val packageName: String

    val simpleName: String

    val members: List<TypeMember>

    val declaredMembers: List<TypeMember>

    fun findMethod(name: String, types: List<ResolvedType>): TypeMember.Method?

    fun findConstructor(types: List<ResolvedType>): TypeMember.Constructor?

    fun findField(name: String): TypeMember.Field?

    /**
     * Returns `this`.
     */
    override fun resolve(context: TypeResolutionContext): ResolvedTypeOrError = this

    /**
     * Returns `true` if `this` [Type] and [other] is compatible.
     *
     * TODO: document what "compatible" means
     */
    // TODO: better name?
    infix fun isCompatible(other: ResolvedTypeOrError): Boolean

    infix fun isIncompatible(other: ResolvedTypeOrError): Boolean = !isCompatible(other)

    infix fun isSameType(other: ResolvedTypeOrError): Boolean

    infix fun isNotSameType(other: ResolvedTypeOrError): Boolean = !isSameType(other)
}