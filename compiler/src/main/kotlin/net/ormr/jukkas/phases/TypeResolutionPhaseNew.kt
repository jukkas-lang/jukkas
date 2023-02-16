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

package net.ormr.jukkas.phases

import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.CompilationUnit
import net.ormr.jukkas.ast.ExpressionStatement
import net.ormr.jukkas.ast.HasType
import net.ormr.jukkas.ast.Import
import net.ormr.jukkas.ast.ImportEntry
import net.ormr.jukkas.ast.Node
import net.ormr.jukkas.ast.Pattern
import net.ormr.jukkas.ast.PatternArgument
import net.ormr.jukkas.ast.StringTemplatePart
import net.ormr.jukkas.type.TypeCache
import net.ormr.jukkas.type.TypeResolutionContext

class TypeResolutionPhaseNew private constructor(source: Source, private val types: TypeCache) : CompilerPhase(source) {
    private val context = object : TypeResolutionContext {
        override val cache: TypeCache
            get() = types

        override fun reportSemanticError(position: Positionable, message: String) {
            this@TypeResolutionPhaseNew.reportSemanticError(position, message)
        }

        override fun reportTypeError(position: Positionable, message: String) {
            this@TypeResolutionPhaseNew.reportTypeError(position, message)
        }
    }

    private fun check(node: Node) {
        when (node) {
            is HasType -> TODO()
            is Import -> TODO("Import")
            is ImportEntry -> TODO("ImportEntry")
            is Pattern -> TODO("Pattern")
            is PatternArgument -> TODO("PatternArgument")
            is ExpressionStatement -> TODO("ExpressionStatement")
            is StringTemplatePart -> TODO("StringTemplatePart")
            is CompilationUnit -> {
                node.imports.forEach(::check)
                node.children.forEach(::check)
            }
        }
    }
}