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

import io.github.classgraph.ArrayTypeSignature
import io.github.classgraph.BaseTypeSignature
import io.github.classgraph.ClassRefTypeSignature
import io.github.classgraph.TypeSignature
import net.ormr.jukkas.type.member.JvmMember

sealed interface JvmType : ResolvedType {
    override val superType: ResolvedType?
    override val interfaces: List<ResolvedType>
    override val declaredMembers: List<JvmMember>

    override fun isSameType(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        // TODO: handle cases with jukkas types, like 'Jukkas.Int = int', and like 'jukkas.Array[Int] = Integer[]'
        else -> this sameJvmDescriptor other
    }

    companion object {
        internal fun fromSignature(signature: TypeSignature): JvmType = when (signature) {
            is ArrayTypeSignature -> JvmReferenceType.from(signature.arrayClassInfo)
            is BaseTypeSignature -> JvmPrimitiveType.fromSymbol(signature.typeSignatureChar.toString())
            is ClassRefTypeSignature -> {
                val info = signature.classInfo ?: error("Could not find ClassInfo for <$signature>")
                JvmReferenceType.from(info)
            }
            else -> throw IllegalArgumentException("Can't create JvmType for <$signature>")
        }
    }
}