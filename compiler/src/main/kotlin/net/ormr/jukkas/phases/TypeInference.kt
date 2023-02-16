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

import net.ormr.jukkas.ast.*
import net.ormr.jukkas.ast.Lambda
import net.ormr.jukkas.type.ErrorType
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.UnknownType

object TypeInference {
    fun inferType(expr: Expression): Type = when (expr) {
        is AssignmentOperation -> inferType(expr.value)
        is BinaryOperation -> {
            val left = inferType(expr.left)
            val right = inferType(expr.right)
            if (left != right) {
                // TODO: we need to check if function exists on types for this...
                errorType(expr, "Incompatible types $left vs $right")
            } else left
        }
        is Block -> TODO()
        is MemberAccessOperation -> TODO()
        is ConditionalBranch -> TODO()
        is Lambda -> inferDefinitionType(expr)
        is DefinitionReference -> expr
            .find(expr.getClosestTable())
            ?.let(::inferDefinitionType) ?: errorType(expr, "Unknown name '${expr.name}'")
        is AnonymousFunctionInvocation -> TODO()
        is InfixInvocation -> TODO()
        is InvocationArgument -> TODO()
        is Literal -> expr.type
        is Return -> expr.type
        is StringTemplateExpression -> expr.type
        else -> TODO()
    }

    fun inferDefinitionType(def: Definition): Type = error("lol")

    /*fun inferDefinitionType(def: Definition): Type = when (def) {
        is Lambda -> resolve(def, def.type, def.body)
        is Function -> TODO()
        is NamedArgument -> def.type
        is Property -> TODO("check initializer or getter if no type given exactly")
        is Variable -> resolve(def, def.type, def.initializer)
    }*/

    private fun resolve(
        parent: Node,
        first: Type,
        fallback: Expression?,
    ): Type = fold(first) { findNullableType(fallback, parent) }

    private fun findNullableType(expr: Expression?, parent: Node): Type =
        expr?.let(::inferType) ?: errorType(parent, "Could not resolve type")

    private inline fun fold(type: Type, ifUnknown: () -> Type): Type = when (type) {
        UnknownType -> ifUnknown()
        else -> type
    }

    private fun errorType(node: Node, message: String): ErrorType {
        node.reportSemanticError(node, message)
        return ErrorType(message)
    }
}