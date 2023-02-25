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

import net.ormr.jukkas.type.member.JvmFunctionComparator
import net.ormr.jukkas.type.member.TypeMember

sealed interface JvmType : ContainerType {
    val clz: Class<*>

    override val functionComparator: Comparator<TypeMember.Function>
        get() = JvmFunctionComparator

    fun toAsmType(): AsmFieldType = AsmFieldType.of(clz)

    fun toJvmDescriptor(): String = getDescriptor(clz)

    companion object {
        private val cache = hashMapOf<String, JvmType>()

        fun of(clz: Class<*>): JvmType = cache.getOrPut(getDescriptor(clz)) {
            when {
                clz.isPrimitive -> JvmPrimitiveType.of(clz)
                clz.isArray -> JvmArrayType(clz)
                JukkasJvmType.isJukkasClass(clz) -> JukkasJvmType.of(clz)
                else -> JvmReferenceType(clz)
            }
        }

        fun find(
            name: String,
            initialize: Boolean = true,
            loader: ClassLoader? = null,
        ): JvmType? = cache.getOrPut(name) {
            try {
                val clz = Class.forName(name, initialize, loader)
                if (JukkasJvmType.isJukkasClass(clz)) JukkasJvmType.of(clz) else JvmReferenceType(clz)
            } catch (_: Exception) {
                return null
            }
        }
    }
}