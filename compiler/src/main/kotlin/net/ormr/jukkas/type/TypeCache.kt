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

import net.ormr.jukkas.Positionable
import net.ormr.jukkas.ast.CompilationUnit
import net.ormr.jukkas.ast.reportSemanticError

class TypeCache internal constructor(private val unit: CompilationUnit) {
    private val entries = hashMapOf<String, ResolvedType>()

    fun find(name: String): ResolvedType? = entries[name]

    fun define(position: Positionable, type: ResolvedType) {
        addType(position, type.simpleName, type)
        if (type.toString() != type.simpleName) {
            addType(position, type.toString(), type)
        }
    }

    private fun addType(position: Positionable, name: String, type: ResolvedType) {
        if (name in entries) {
            unit.reportSemanticError(position, "Redefining name: $name")
        } else {
            entries[name] = type
        }
    }
}