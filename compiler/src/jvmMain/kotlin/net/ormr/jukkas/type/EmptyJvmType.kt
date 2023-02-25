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

// TODO: better name
sealed interface EmptyJvmType : JvmType {
    override val superType: ContainerType?
        get() = null

    override val interfaces: List<ContainerType>
        get() = emptyList()

    override val isObject: Boolean
        get() = false

    override val isInterface: Boolean
        get() = false

    override val members: List<TypeMember>
        get() = emptyList()

    override val declaredMembers: List<TypeMember>
        get() = emptyList()

    override fun findFunction(name: String, parameterTypes: List<Type>): TypeMember.Function? = null

    override fun findProperty(name: String): TypeMember.Property? = null
}