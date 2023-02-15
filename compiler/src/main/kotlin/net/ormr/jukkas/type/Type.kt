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

import net.ormr.jukkas.StructurallyComparable

sealed interface Type : StructurallyComparable {
    val internalName: String

    val jvmName: String
        get() = internalName.replace('.', '$').replace('/', '.')

    infix fun jvmDescriptorMatches(other: Type): Boolean = toJvmDescriptor() == other.toJvmDescriptor()

    fun resolve(context: TypeResolutionContext): ResolvedTypeOrError

    fun toAsmType(): AsmType = AsmReferenceType.fromDescriptor(toJvmDescriptor())

    fun toJvmDescriptor(): String

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean = equals(other)
}