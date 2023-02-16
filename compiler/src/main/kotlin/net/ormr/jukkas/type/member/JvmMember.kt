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

import net.ormr.jukkas.type.AsmMethodType
import net.ormr.jukkas.type.ResolvedType
import net.ormr.krautils.collections.asUnmodifiableList
import java.lang.reflect.Constructor as JavaConstructor
import java.lang.reflect.Executable as JavaExecutable
import java.lang.reflect.Field as JavaField
import java.lang.reflect.Method as JavaMethod

sealed interface JvmMember : TypeMember {
    data class Method(val method: JavaMethod) : TypeMember.Method, JvmMember {
        override val name: String
            get() = method.name

        override val parameterTypes: List<ResolvedType> by lazy { createTypeList(method) }

        override val returnType: ResolvedType by lazy { ResolvedType.of(method.returnType) }

        override fun toAsmType(): AsmMethodType = AsmMethodType.of(method)
    }

    data class Constructor(val constructor: JavaConstructor<*>) : TypeMember.Constructor, JvmMember {
        override val name: String
            get() = constructor.name

        override val parameterTypes: List<ResolvedType> by lazy { createTypeList(constructor) }

        override fun toAsmType(): AsmMethodType = AsmMethodType.of(constructor)
    }

    data class Field(val field: JavaField) : TypeMember.Field, JvmMember {
        override val name: String
            get() = this.field.name

        override val type: ResolvedType by lazy { ResolvedType.of(field.type) }
    }
}

private fun createTypeList(executable: JavaExecutable): List<ResolvedType> =
    executable.parameterTypes.map { ResolvedType.of(it) }.asUnmodifiableList()