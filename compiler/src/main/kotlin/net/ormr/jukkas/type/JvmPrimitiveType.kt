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

enum class JvmPrimitiveType(
    val clz: Class<*>,
    val boxedType: JvmReferenceType,
    override val internalName: String,
) : JvmType {
    // TODO: this isn't entirely correct
    VOID(primitive<Void>(), JvmReferenceType.VOID, "jukkas/Unit"),
    BOOLEAN(primitive<Boolean>(), JvmReferenceType.BOOLEAN, "jukkas/Boolean"),
    CHAR(primitive<Char>(), JvmReferenceType.CHAR, "jukkas/Char"),
    BYTE(primitive<Byte>(), JvmReferenceType.BYTE, "jukkas/Int8"),
    SHORT(primitive<Short>(), JvmReferenceType.SHORT, "jukkas/Int16"),
    INT(primitive<Int>(), JvmReferenceType.INT, "jukkas/Int32"),
    LONG(primitive<Long>(), JvmReferenceType.LONG, "jukkas/Int64"),
    FLOAT(primitive<Float>(), JvmReferenceType.FLOAT, "jukkas/Float32"),
    DOUBLE(primitive<Double>(), JvmReferenceType.DOUBLE, "jukkas/Float64");

    override val simpleName: String
        get() = clz.name

    override val packageName: String
        get() = "java/lang"

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

    override fun findMethod(name: String, types: List<ResolvedTypeOrError>): TypeMember.Method? = null

    override fun findConstructor(types: List<ResolvedTypeOrError>): TypeMember.Constructor? = null

    override fun findField(name: String): TypeMember.Field? = null

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isCompatible -> JukkasType")
        is JvmType -> when (other) {
            is JvmArrayType -> false
            is JvmPrimitiveType -> this == other
            is JvmReferenceType -> boxedType isCompatible other
        }
    }

    override fun compareCompatibility(other: ResolvedTypeOrError): Int = when (other) {
        is ErrorType -> 0
        is JukkasType -> TODO("JukkasType")
        is JvmType -> when (other) {
            is JvmArrayType -> 0
            // TODO: is this proper for allowing primitives to be passed in to wrappers?
            is JvmPrimitiveType -> boxedType.compareCompatibility(other.boxedType)
            is JvmReferenceType -> boxedType.compareCompatibility(other)
        }
    }

    override fun toJvmDescriptor(): String = clz.descriptorString()

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