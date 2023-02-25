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

package net.ormr.jukkas.type.member

import net.ormr.jukkas.ast.Visibility
import net.ormr.krautils.reflection.isPrivate
import net.ormr.krautils.reflection.isProtected
import net.ormr.krautils.reflection.isPublic

internal typealias JavaMember = java.lang.reflect.Member
internal typealias JavaField = java.lang.reflect.Field
internal typealias JavaMethod = java.lang.reflect.Method
internal typealias JavaConstructor<T> = java.lang.reflect.Constructor<T>

internal fun getVisibility(member: JavaMember): Visibility = when {
    member.isPublic -> Visibility.PUBLIC
    member.isProtected -> Visibility.PROTECTED
    member.isPrivate -> Visibility.PRIVATE
    // if none of the above branches match, then it's package private
    else -> Visibility.PACKAGE_PRIVATE
}