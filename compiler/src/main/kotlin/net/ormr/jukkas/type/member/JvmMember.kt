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

import io.github.classgraph.FieldInfo
import io.github.classgraph.MethodInfo
import net.ormr.jukkas.type.AsmMethodType
import net.ormr.jukkas.type.JvmReferenceType
import net.ormr.jukkas.type.JvmType
import net.ormr.jukkas.type.ResolvedType

sealed interface JvmMember : TypeMember {
    data class Method(val info: MethodInfo) : TypeMember.Method, JvmMember {
        override val name: String
            get() = info.name

        override val parameterTypes: List<ResolvedType> by lazy { createTypeList(info) }

        // TODO: is this valid?
        override fun toAsmType(): AsmMethodType = AsmMethodType.fromDescriptor(info.typeDescriptorStr)
    }

    data class Constructor(val info: MethodInfo) : TypeMember.Constructor, JvmMember {
        override val name: String
            get() = info.name

        override val parameterTypes: List<ResolvedType> by lazy { createTypeList(info) }

        // TODO: is this valid?
        override fun toAsmType(): AsmMethodType = AsmMethodType.fromDescriptor(info.typeDescriptorStr)
    }

    data class Field(val info: FieldInfo) : TypeMember.Field, JvmMember {
        override val name: String
            get() = this.info.name

        override val type: ResolvedType by lazy { JvmReferenceType.from(info.classInfo) }
    }
}

private fun createTypeList(method: MethodInfo): List<ResolvedType> =
    method.parameterInfo.map { JvmType.fromSignature(it.typeDescriptor) }