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
import net.ormr.krautils.collections.getOrThrow
import kotlin.reflect.typeOf

enum class JvmPrimitiveType(val clz: Class<*>) : JvmType {
    VOID(primitive<Void>()),
    BOOLEAN(primitive<Boolean>()),
    CHAR(primitive<Char>()),
    BYTE(primitive<Byte>()),
    SHORT(primitive<Short>()),
    INT(primitive<Int>()),
    LONG(primitive<Long>()),
    FLOAT(primitive<Float>()),
    DOUBLE(primitive<Double>());

    // TODO: internal name for primitive should probably be the actual name we use in Jukkas
    //       so like: int -> Int32, long -> Int64, etc..
    //       this will cause a problem for the VOID type tho, because Jukkas doesn't have the void type
    override val internalName: String
        get() = getDescriptor(clz)

    override val simpleName: String
        get() = clz.name

    override val packageName: String
        get() = clz.packageName

    override val jvmName: String
        get() = getDescriptor(clz)

    override val superType: ResolvedType?
        get() = null

    override val interfaces: List<ResolvedType>
        get() = emptyList()

    override val members: List<TypeMember>
        get() = emptyList()

    override val declaredMembers: List<JvmMember>
        get() = emptyList()

    override fun findMethod(name: String, types: List<ResolvedType>): TypeMember.Method? = null

    override fun findConstructor(types: List<ResolvedType>): TypeMember.Constructor? = null

    override fun findField(name: String): TypeMember.Field? = null

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isCompatible -> JukkasType")
        is JvmType -> when (other) {
            is JvmArrayType -> false
            is JvmPrimitiveType -> this == other
            is JvmReferenceType -> TODO("allow primitives in place of wrapper types")
        }
    }

    override fun toJvmDescriptor(): String = internalName

    override fun toAsmType(): AsmPrimitiveType = AsmPrimitiveType.of(clz)

    override fun toString(): String = internalName

    companion object {
        private val cache: Map<Class<*>, JvmPrimitiveType> = values().associateByTo(hashMapOf()) { it.clz }

        // TODO: we currently don't support implicit conversions of wrapper classes to their primitive counterparts
        //       should we do this?
        fun of(clz: Class<*>): JvmPrimitiveType = cache.getOrThrow(clz) { "Class <$clz> is not a primitive" }
    }
}

private inline fun <reified T : Any> primitive(): Class<T> =
    T::class.javaPrimitiveType ?: error("Could not retrieve Java primitive type for ${typeOf<T>()}")