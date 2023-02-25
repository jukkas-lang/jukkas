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

import net.ormr.krautils.collections.getOrThrow
import kotlin.reflect.typeOf

enum class JvmPrimitiveType(
    override val clz: Class<*>,
    private val boxedName: String,
    override val simpleName: String,
) : EmptyJvmType {
    VOID(primitive<Void>(), "java.lang.Void", "Unit"),
    BOOLEAN(primitive<Boolean>(), "java.lang.Boolean", "Boolean"),
    CHAR(primitive<Char>(), "java.lang.Char", "Char"),
    BYTE(primitive<Byte>(), "java.lang.Byte", "Int8"),
    SHORT(primitive<Short>(), "java.lang.Short", "Int16"),
    INT(primitive<Int>(), "java.lang.Int", "Int32"),
    LONG(primitive<Long>(), "java.lang.Long", "Int64"),
    FLOAT(primitive<Float>(), "java.lang.Float", "Float32"),
    DOUBLE(primitive<Double>(), "java.lang.Double", "Float64");

    override val qualifiedName: String = "jukkas/$simpleName"

    val boxedType: JvmReferenceType by lazy {
        val type = JvmType.find(boxedName) ?: error("Can't find boxed type <$boxedName> for primitive <$name>")
        (type as? JvmReferenceType) ?: error("Boxed type for primitive <$name> is not <JvmReferenceType> but <$type>")
    }

    override fun isCompatibleWith(other: Type): Boolean {
        if (this isSameType other) return true
        return when (other) {
            is JvmType -> when (other) {
                is JvmPrimitiveType -> this == other
                is JvmReferenceType -> boxedType isCompatibleWith other
                is JvmArrayType, is JukkasJvmType -> false
            }
            // TODO: allow jukkas types to be used as primitives
            else -> super.isCompatibleWith(other)
        }
    }

    override fun compareCompatibilityTo(other: Type): Int = when (other) {
        is JvmType -> when (other) {
            // TODO: is this proper for allowing primitives to be passed in to wrappers?
            is JvmPrimitiveType -> boxedType compareCompatibilityTo other.boxedType
            is JvmReferenceType -> boxedType compareCompatibilityTo other
            is JvmArrayType, is JukkasJvmType -> 0
        }
        // TODO: allow jukkas types to be used as primitives
        else -> super.compareCompatibilityTo(other)
    }

    override fun asString(): String = qualifiedName

    private val asmType = AsmPrimitiveType.of(clz)

    override fun toAsmType(): AsmPrimitiveType = asmType

    companion object {
        private val cache: Map<Class<*>, JvmPrimitiveType> = values().associateByTo(hashMapOf()) { it.clz }

        // TODO: we currently don't support implicit conversions of wrapper classes to their primitive counterparts
        //       should we do this?
        fun of(clz: Class<*>): JvmPrimitiveType = cache.getOrThrow(clz) { "Class <$clz> is not a primitive" }
    }
}

private inline fun <reified T : Any> primitive(): Class<T> =
    T::class.javaPrimitiveType ?: error("Could not retrieve Java primitive type for ${typeOf<T>()}")