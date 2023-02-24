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
import net.ormr.jukkas.type.Type

@Suppress("UnnecessaryAbstractClass")
sealed interface TypeMember {
    val declaringType: Type
    val visibility: Visibility
    val name: String

    val isStatic: Boolean // for java interop

    sealed interface HasType : TypeMember {
        fun findType(): Type
    }

    abstract class Property : TypeMember, HasType {
        abstract val getter: Getter
        abstract val setter: Setter?

        open val type: Type
            get() = getter.returnType

        override fun findType(): Type = type
    }

    interface Getter : TypeMember, HasType {
        val returnType: Type

        override fun findType(): Type = returnType
    }

    interface Setter : TypeMember

    sealed interface Executable : TypeMember, HasType {
        val parameterTypes: List<Type>
        val returnType: Type

        override fun findType(): Type = returnType
    }

    abstract class Function : Executable

    abstract class Constructor : Executable {
        override val isStatic: Boolean
            get() = false

        override val returnType: Type
            get() = declaringType
    }
}

val TypeMember.isPublic: Boolean
    get() = visibility == Visibility.PUBLIC

val TypeMember.isProtected: Boolean
    get() = visibility == Visibility.PROTECTED

val TypeMember.isPrivate: Boolean
    get() = visibility == Visibility.PRIVATE

val TypeMember.isPackagePrivate: Boolean
    get() = visibility == Visibility.PACKAGE_PRIVATE