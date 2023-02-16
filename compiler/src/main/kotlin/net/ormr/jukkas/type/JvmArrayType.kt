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

import net.ormr.jukkas.type.member.JvmMember
import net.ormr.jukkas.type.member.TypeMember
import net.ormr.jukkas.utils.getDescriptor

class JvmArrayType private constructor(val clz: Class<*>) : JvmType {
    override val superType: ResolvedType?
        get() = null

    override val interfaces: List<ResolvedType>
        get() = emptyList()

    val componentType: ResolvedType by lazy {
        val type = clz.componentType ?: error("Supposed JvmArrayType class <$clz> has no componentType")
        JvmType.of(type)
    }

    // TODO: internal name for array needs to be reworked
    override val internalName: String = "${packageName.replace('.', '/')}/${simpleName.replace('$', '.')}"

    // empty if located in root package
    override val packageName: String
        get() = clz.packageName

    override val simpleName: String
        get() = clz.simpleName

    // TODO: verify that array classes don't actually contain any members

    override val members: List<TypeMember>
        get() = emptyList()

    override val declaredMembers: List<JvmMember>
        get() = emptyList()

    override fun findMethod(name: String, types: List<ResolvedTypeOrError>): JvmMember.Method? = null

    override fun findConstructor(types: List<ResolvedTypeOrError>): JvmMember.Constructor? = null

    override fun findField(name: String): JvmMember.Field? = null

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isCompatible -> JukkasType")
        is JvmType -> when (other) {
            // TODO: I don't think this is fully safe way of doing this, as arrays in Java behave really weirdly
            //       with what can go in them due to the type variance they exhibit
            is JvmArrayType -> other.clz.isAssignableFrom(clz)
            is JvmPrimitiveType, is JvmReferenceType -> false
        }
    }

    override fun toJvmDescriptor(): String = getDescriptor(clz)

    override fun toAsmType(): AsmArrayType = AsmArrayType.of(clz)

    override fun toString(): String = internalName

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmArrayType -> false
        clz != other.clz -> false
        else -> true
    }

    override fun hashCode(): Int = clz.hashCode()

    companion object {
        private val cache = hashMapOf<String, JvmArrayType>()

        // TODO: 'info.name' is probably not correct as we want to use the full name?
        fun of(clz: Class<*>): JvmArrayType {
            require(!clz.isPrimitive) { "Class <$clz> is a primitive, use JvmPrimitiveType instead" }
            require(clz.isArray) { "Class <$clz> is not an array" }
            return cache.getOrPut(getDescriptor(clz)) { JvmArrayType(clz) }
        }
    }
}