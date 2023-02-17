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
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.ast.Function
import net.ormr.jukkas.type.ResolvedTypeOrError
import net.ormr.jukkas.type.Type

class TypeCheckingPhase private constructor(source: Source) : CompilerPhase(source) {
    private fun typeCheck(node: Node) {
        when (node) {
            is CompilationUnit -> checkTypes(node.children)
            is DefaultArgument -> {
                val (_, type, default) = node
                typeCheck(default)
                checkCompatibility(default, type, default.type)
            }
            is AssignmentOperation -> {
                val (left, _, value) = node
                typeCheck(left)
                typeCheck(value)
                checkCompatibility(value, left.type, value.type)
            }
            is BinaryOperation -> {
                val (left, _, right) = node
                typeCheck(left)
                typeCheck(right)
                checkCompatibility(node, left.type, right.type)
            }
            is Block -> TODO("Block")
            is ConditionalBranch -> TODO("ConditionalBranch")
            is AnonymousFunctionInvocation -> TODO("AnonymousFunctionInvocation")
            is FunctionInvocation -> TODO("FunctionInvocation")
            is InfixInvocation -> TODO("InfixInvocation")
            is Lambda -> TODO("Lambda")
            is MemberAccessOperation -> TODO("MemberAccessOperation")
            is StringTemplateExpression -> TODO("StringTemplateExpression")
            is ExpressionStatement -> typeCheck(node.expression)
            is Function -> TODO("Function")
            is LocalVariable -> {
                val (_, _, type, initializer) = node
                if (initializer != null) {
                    typeCheck(initializer)
                    checkCompatibility(initializer, type, initializer.type)
                }
            }
            is Property -> TODO("Property")
            is StringTemplatePart -> TODO("StringTemplatePart")
            // unreachable
            is Import -> unreachable<Import>()
            is ImportEntry -> unreachable<ImportEntry>()
            // nothing to type check
            is Pattern -> {}
            is BasicArgument -> {}
            is PatternArgument -> {}
            is DefinitionReference -> {}
            is BooleanLiteral -> {}
            is IntLiteral -> {}
            is StringLiteral -> {}
            is SymbolLiteral -> {}
            is InvocationArgument -> {}
            // type checking of return expressions is done where they're placed
            is Return -> {}
        }
    }

    private fun <T : Node> checkTypes(nodes: List<T>) {
        nodes.forEach(::typeCheck)
    }

    private fun checkCompatibility(position: Positionable, a: Type, b: Type) {
        require(a is ResolvedTypeOrError) { notResolved(a) }
        require(b is ResolvedTypeOrError) { notResolved(b) }
        if (a isIncompatible b) {
            reportIncompatibleTypes(position, a, b)
        }
    }

    private fun notResolved(type: Type): String = "Type <$type> is not resolved. Was type resolution skipped?"

    private fun reportIncompatibleTypes(
        position: Positionable,
        expected: Type,
        got: Type,
    ) {
        reportTypeError(position, formatIncompatibleTypes(expected, got))
    }
}