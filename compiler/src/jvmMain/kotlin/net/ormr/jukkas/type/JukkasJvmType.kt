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

import net.ormr.jukkas.type.member.TypeMember

class JukkasJvmType private constructor(override val clz: Class<*>) : JukkasType, JvmType {
    override val qualifiedName: String
        get() = TODO("Not yet implemented")
    override val simpleName: String
        get() = TODO("Not yet implemented")
    override val superType: ContainerType?
        get() = TODO("Not yet implemented")
    override val interfaces: List<ContainerType>
        get() = TODO("Not yet implemented")
    override val members: List<TypeMember>
        get() = TODO("Not yet implemented")
    override val declaredMembers: List<TypeMember>
        get() = TODO("Not yet implemented")

    override val isObject: Boolean
        get() = TODO("Not yet implemented")
    override val isInterface: Boolean
        get() = TODO("Not yet implemented")

    override fun findProperty(name: String): TypeMember.Property? {
        TODO("Not yet implemented")
    }

    override fun asString(): String {
        TODO("Not yet implemented")
    }

    companion object {
        // TODO: check for annotation
        @Suppress("FunctionOnlyReturningConstant", "UnusedPrivateMember")
        fun isJukkasClass(clz: Class<*>): Boolean = false

        @Suppress("UnusedPrivateMember")
        fun of(clz: Class<*>): JukkasJvmType = TODO("JukkasJvmType")
    }
}