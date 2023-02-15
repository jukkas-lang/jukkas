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
import net.ormr.krautils.collections.getOrThrow

class JvmPrimitiveType private constructor(
    override val internalName: String,
    override val simpleName: String,
) : JvmType {
    override val superType: ResolvedType?
        get() = null

    override val interfaces: List<ResolvedType>
        get() = emptyList()

    override val members: List<TypeMember>
        get() = emptyList()

    override val declaredMembers: List<JvmMember>
        get() = emptyList()

    override val packageName: String
        get() = ""

    override fun findMethod(name: String, types: List<ResolvedType>): TypeMember.Method? = null

    override fun findConstructor(types: List<ResolvedType>): TypeMember.Constructor? = null

    override fun findField(name: String): TypeMember.Field? = null

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isCompatible -> JukkasType")
        is JvmType -> when (other) {
            is JvmPrimitiveType -> internalName == other.internalName
            is JvmReferenceType -> TODO("allow primitives in place of wrapper types")
        }
    }

    override fun toJvmDescriptor(): String = internalName

    override fun toAsmType(): AsmPrimitiveType = AsmPrimitiveType.fromDescriptor(internalName)

    companion object {
        private val symbolCache = hashMapOf<String, JvmPrimitiveType>()
        private val nameCache = hashMapOf<String, JvmPrimitiveType>()

        val VOID: JvmPrimitiveType = of("V", "void")
        val BOOLEAN: JvmPrimitiveType = of("Z", "boolean")
        val CHAR: JvmPrimitiveType = of("C", "char")
        val BYTE: JvmPrimitiveType = of("B", "byte")
        val SHORT: JvmPrimitiveType = of("S", "short")
        val INT: JvmPrimitiveType = of("I", "int")
        val LONG: JvmPrimitiveType = of("J", "long")
        val FLOAT: JvmPrimitiveType = of("F", "float")
        val DOUBLE: JvmPrimitiveType = of("D", "double")

        private fun of(symbol: String, name: String): JvmPrimitiveType {
            val type = JvmPrimitiveType(symbol, name)
            symbolCache[symbol] = type
            nameCache[name] = type
            return type
        }

        /**
         * Returns the [JvmPrimitiveType] corresponding to the given [symbol], or throws a [NoSuchElementException] if
         * none is found.
         *
         * For example, [INT] has the symbol `I`, and [BOOLEAN] has the symbol `Z`.
         */
        fun fromSymbol(symbol: String): JvmPrimitiveType =
            symbolCache.getOrThrow(symbol) { "Unknown primitive type symbol: $symbol" }

        /**
         * Returns the [JvmPrimitiveType] corresponding to the given [name], or throws a [NoSuchElementException] if
         * none is found.
         *
         * For example, [INT] has the name `int`, and [BOOLEAN] has the name `boolean`.
         */
        fun fromName(name: String): JvmPrimitiveType =
            nameCache.getOrThrow(name) { "Unknown primitive type name: $name" }
    }
}