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
import net.ormr.jukkas.StructurallyComparable

class TypeName(val position: Position, override val internalName: String) : Positionable, Type {
    override fun findPositionOrNull(): Position = position

    // TODO: handle jukkas classes
    override fun resolve(context: TypeResolutionContext): ResolvedTypeOrError =
        context.cache.find(internalName)
            ?: context.errorType(this, "Can't find type '$internalName'")

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is TypeName && internalName == other.internalName

    override fun toString(): String = "TypeName(internalName='$internalName')"

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is TypeName -> false
        internalName != other.internalName -> false
        else -> true
    }

    override fun hashCode(): Int = internalName.hashCode()
}