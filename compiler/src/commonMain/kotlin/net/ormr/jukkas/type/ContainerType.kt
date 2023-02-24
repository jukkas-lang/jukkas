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
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ContainerType : Type {
    val qualifiedName: String
    val simpleName: String

    val isObject: Boolean
    val isInterface: Boolean

    val superType: ContainerType?
    val interfaces: List<ContainerType>

    val members: List<TypeMember>
    val declaredMembers: List<TypeMember>

    val functionComparator: Comparator<TypeMember.Function>

    // TODO: is this sane?
    infix fun isSuperTypeOf(other: ContainerType): Boolean {
        if (other.superType?.let { this isSameType it } == true) return true
        other.walkHierarchy {
            if (this isSameType it) return true
        }
        return false
    }

    infix fun isSubTypeOf(other: ContainerType): Boolean = other.isSuperTypeOf(this)

    override fun isCompatibleWith(other: Type): Boolean {
        if (this isSameType other) return true
        return when (other) {
            is ContainerType -> this isSuperTypeOf other
        }
    }

    override fun compareCompatibilityTo(other: Type): Int = when (other) {
        is ContainerType -> when {
            this isSuperTypeOf other -> 1
            this isSubTypeOf other -> -1
            else -> 0
        }
    }

    fun findProperty(name: String): TypeMember.Property?

    fun findFunction(name: String, parameterTypes: List<Type>): TypeMember.Function? {
        walkHierarchy { type ->
            type.declaredMembers
                .asSequence()
                .filterIsInstance<TypeMember.Function>()
                .filter { it.name == name }
                .filter { argumentsMatch(it, parameterTypes) }
                .sortedWith(functionComparator)
                .firstOrNull()
                ?.let { return it }
        }
        return null
    }

    fun findConstructor(parameterTypes: List<Type>): TypeMember.Constructor? =
        findDeclaredMember { typesMatch(parameterTypes, it.parameterTypes) }

    fun argumentsMatch(function: TypeMember.Executable, parameterTypes: List<Type>): Boolean =
        typesMatch(function.parameterTypes, parameterTypes)

    fun typesMatch(a: List<Type>, b: List<Type>): Boolean {
        if (a.size != b.size) return false

        for (i in a.indices) {
            if (a[i] isIncompatible b[i]) {
                return false
            }
        }

        return true
    }
}

/**
 * Walks up the hierarchy chain of `this` [ContainerType] invoking [action] on its
 * [superType][ContainerType.superType]s and [interfaces][ContainerType.interfaces].
 *
 * Note that the first invocation of [action] will be with `this` as its argument.
 *
 * The [superType][ContainerType.superType] will be passed in before any of the
 * [interfaces][ContainerType.interfaces] get passed in to `action`. This means that `superType` has higher
 * priority than `interfaces`.
 */
inline fun ContainerType.walkHierarchy(action: (ContainerType) -> Unit) {
    var current: ContainerType? = this
    while (current != null) {
        action(current)
        for (iface in interfaces) {
            action(iface)
        }
        current = current.superType
    }
}

/**
 * Returns the first member that matches [predicate], or `null` if none is found.
 *
 * Note that this uses the [walkHierarchy] function to traverse the hierarchy, rather than using the
 * [members][ContainerType.members] list. This is so that we can guarantee that we get the closest match possible.
 *
 * @see [findDeclaredMember]
 * @see [walkHierarchy]
 */
inline fun <reified T : TypeMember> ContainerType.findMember(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.UNKNOWN)
    }

    walkHierarchy { type ->
        for (member in type.declaredMembers) {
            if (member !is T) continue
            if (predicate(member)) return member
        }
    }
    return null
}

/**
 * Returns the first declared member from [declaredMembers][ContainerType.declaredMembers] that matches
 * [predicate], or `null` if none is found.
 *
 * @see [findMember]
 */
inline fun <reified T : TypeMember> ContainerType.findDeclaredMember(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.UNKNOWN)
    }

    for (member in declaredMembers) {
        if (member !is T) continue
        if (predicate(member)) return member
    }

    return null
}