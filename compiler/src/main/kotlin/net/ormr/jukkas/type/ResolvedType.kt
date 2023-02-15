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

sealed interface ResolvedType : ResolvedTypeOrError

/**
 * Returns the first member that matches [predicate], or `null` if none is found.
 *
 * Note that this uses the [walkHierarchy] function to traverse the hierarchy, rather than using the
 * [members][ResolvedTypeOrError.members] list. This is so that we can guarantee that we get the closest match possible.
 *
 * @see [findDeclaredMember]
 * @see [walkHierarchy]
 */
inline fun <reified T : TypeMember> ResolvedType.findMember(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.UNKNOWN)
    }

    walkHierarchy {
        for (member in declaredMembers) {
            if (member !is T) continue
            if (predicate(member)) return member
        }
    }
    return null
}

/**
 * Returns the first declared member from [declaredMembers][ResolvedTypeOrError.declaredMembers] that matches
 * [predicate], or `null` if none is found.
 *
 * @see [findMember]
 */
inline fun <reified T : TypeMember> ResolvedType.findDeclaredMember(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.UNKNOWN)
    }

    for (member in declaredMembers) {
        if (member !is T) continue
        if (predicate(member)) return member
    }

    return null
}

/**
 * Walks up the hierarchy chain of `this` [ResolvedTypeOrError] invoking [action] on its
 * [superType][ResolvedTypeOrError.superType]s and [interfaces][ResolvedTypeOrError.interfaces].
 *
 * Note that the first invocation of [action] will be with `this` as its argument.
 *
 * The [superType][ResolvedTypeOrError.superType] will be passed in before any of the
 * [interfaces][ResolvedTypeOrError.interfaces] get passed in to `action`. This means that `superType` has higher
 * priority than `interfaces`.
 */
inline fun ResolvedType.walkHierarchy(action: (ResolvedTypeOrError) -> Unit) {
    var current: ResolvedTypeOrError? = this
    while (current != null) {
        action(current)
        for (iface in interfaces) {
            action(iface)
        }
        current = current.superType
    }
}