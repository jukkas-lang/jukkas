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

package net.ormr.jukkas.type.member

import net.ormr.jukkas.ast.Visibility
import net.ormr.jukkas.type.ContainerType
import net.ormr.jukkas.type.JvmType
import net.ormr.jukkas.type.Type
import net.ormr.krautils.reflection.isFinal
import net.ormr.krautils.reflection.isStatic

class JvmField(val member: JavaField) : TypeMember.Property(), TypeMember.Getter, TypeMember.Setter, JvmTypeMember {
    override val declaringType: ContainerType by lazy { JvmType.of(member.declaringClass) }

    override val visibility: Visibility by lazy { getVisibility(member) }

    override val name: String = member.name

    override val getter: TypeMember.Getter
        get() = this

    override val setter: TypeMember.Setter?
        get() = if (member.isFinal) null else this

    override val returnType: Type by lazy { JvmType.of(member.type) }

    override val isStatic: Boolean
        get() = member.isStatic

    override fun findType(): Type = type

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmField -> false
        member != other.member -> false
        else -> true
    }

    override fun hashCode(): Int = member.hashCode()

    override fun toString(): String = "JvmField(member=$member)"
}