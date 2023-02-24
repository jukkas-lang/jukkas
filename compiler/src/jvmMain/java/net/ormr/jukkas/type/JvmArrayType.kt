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

class JvmArrayType internal constructor(override val clz: Class<*>) : EmptyJvmType {
    init {
        require(!clz.isPrimitive) { "Class <$clz> is a primitive, use JvmPrimitiveType instead" }
        require(clz.isArray) { "Class <$clz> is not an array type" }
    }

    val componentType: ContainerType by lazy {
        val type = clz.componentType ?: error("Supposed JvmArrayType class <$clz> has no componentType")
        JvmType.of(type)
    }

    override val qualifiedName: String by lazy { "jukkas/$simpleName" }

    override val simpleName: String by lazy { "Array[${componentType.simpleName}]" }

    override fun isCompatibleWith(other: Type): Boolean {
        if (this isSameType other) return true
        return when (other) {
            is JvmType -> when (other) {
                // TODO: is this sane?
                is JvmArrayType -> componentType isCompatibleWith other.componentType
                // TODO: allow the Jukkas array type to be passed in and out jvm array types via auto boxing
                is JvmPrimitiveType, is JukkasJvmType, is JvmReferenceType -> false
            }
            else -> super.isCompatibleWith(other)
        }
    }

    override fun compareCompatibilityTo(other: Type): Int = when (other) {
        is JvmType -> when (other) {
            // TODO: is this sane?
            is JvmArrayType -> componentType compareCompatibilityTo other.componentType
            // TODO: allow the Jukkas array type to be passed in and out jvm array types via auto boxing
            is JvmPrimitiveType, is JukkasJvmType, is JvmReferenceType -> 0
        }
        else -> super.compareCompatibilityTo(other)
    }

    override fun asString(): String = qualifiedName

    private val cachedAsmType by lazy { AsmArrayType.of(clz) }

    override fun toAsmType(): AsmArrayType = cachedAsmType
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmArrayType -> false
        clz != other.clz -> false
        else -> true
    }

    override fun hashCode(): Int = clz.hashCode()

    override fun toString(): String = "JvmArrayType(clz=$clz)"
}