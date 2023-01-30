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

import net.ormr.jukkas.Position
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.utils.scanForClass

class TypeName(val position: Position, val name: String) : Type, Positionable {
    override val jvmName: String
        get() = name.replace('.', '/')

    override fun findPositionOrNull(): Position = position

    // TODO: handle jukkas classes
    override fun resolve(context: TypeResolutionContext): ResolvedType = context.cache.find(name) ?: run {
        val info = scanForClass(name) ?: return@run run {
            context.reportSemanticError(position, "Can't find type '$name'")
            ErrorType("Can't find type '$name'")
        }
        JvmType(info)
    }

    override fun toDescriptor(): String = "L${jvmName};"

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is TypeName -> false
        position != other.position -> false
        name != other.name -> false
        else -> true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}