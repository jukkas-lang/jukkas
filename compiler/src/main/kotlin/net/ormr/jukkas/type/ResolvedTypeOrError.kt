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

    val isInterface: Boolean
        get() = false

    fun findMethod(name: String, types: List<ResolvedTypeOrError>): TypeMember.Method?

    /**
     * Returns a list of all the methods that match [name].
     */
    fun findMethods(name: String): List<TypeMember.Method> = emptyList()

    fun findConstructor(types: List<ResolvedTypeOrError>): TypeMember.Constructor?

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

    /**
     * Compares `this` type with the [other] type.
     *
     * If `this` and `other` are *not* comparable, `0` is returned, this is so that the order is not changed
     * in a stable sort.
     */
    fun compareCompatibility(other: ResolvedTypeOrError): Int

    infix fun isIncompatible(other: ResolvedTypeOrError): Boolean = !isCompatible(other)

    infix fun isSameType(other: ResolvedTypeOrError): Boolean

    infix fun isNotSameType(other: ResolvedTypeOrError): Boolean = !isSameType(other)

    companion object {
        val COMPARATOR: Comparator<ResolvedTypeOrError> = Comparator { o1, o2 -> o1.compareCompatibility(o2) }
    }
}