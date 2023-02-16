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

import net.ormr.jukkas.type.member.JukkasMember
import net.ormr.jukkas.type.member.TypeMember

class JukkasType private constructor(override val internalName: String) : ResolvedType {
    override val superType: ResolvedType?
        get() = TODO("Not yet implemented")

    override val interfaces: List<ResolvedType>
        get() = TODO("Not yet implemented")

    override val packageName: String
        get() = TODO("Not yet implemented")
    override val simpleName: String
        get() = TODO("Not yet implemented")

    override val members: List<JukkasMember>
        get() = TODO("Not yet implemented")

    override val declaredMembers: List<JukkasMember>
        get() = TODO("Not yet implemented")

    override fun findMethod(name: String, types: List<ResolvedTypeOrError>): TypeMember.Method? {
        TODO("Not yet implemented")
    }

    override fun findConstructor(types: List<ResolvedTypeOrError>): TypeMember.Constructor? {
        TODO("Not yet implemented")
    }

    override fun findField(name: String): TypeMember.Field? {
        TODO("Not yet implemented")
    }

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isCompatible -> JukkasType")
        is JvmType -> TODO("isCompatible -> JukkasType")
    }

    override fun isSameType(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isSameType -> JukkasType")
        is JvmType -> TODO("isSameType -> JukkasType")
    }

    override fun toJvmDescriptor(): String {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JukkasType -> false
        internalName != other.internalName -> false
        else -> true
    }

    override fun hashCode(): Int = internalName.hashCode()

    companion object {
        val UNIT = JukkasType("jukkas.Unit")
    }
}