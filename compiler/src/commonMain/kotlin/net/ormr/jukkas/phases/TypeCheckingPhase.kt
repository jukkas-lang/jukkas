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

import net.ormr.jukkas.JukkasResult
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeOrError
import net.ormr.jukkas.type.isIncompatible
import net.ormr.jukkas.utils.ifNotNull
import kotlin.jvm.JvmName

class TypeCheckingPhase private constructor(source: Source) : CompilerPhase(source) {
    @JvmName("nullableCheckType")
    private fun <T : Node> checkType(node: T?): T? = node?.let(::checkType)

    @Suppress("CyclomaticComplexMethod")
    private fun <T : Node> checkType(node: T): T {
        when (node) {
            is CompilationUnit -> checkTypes(node.children)
            is DefaultArgument -> {
                val type = node.type
                val default = checkType(node.default)
                checkCompatibility(default, type.resolvedType, default.resolvedType)
            }
            is AssignmentOperation -> {
                val left = checkType(node.left)
                val value = checkType(node.value)
                checkCompatibility(value, left.resolvedType, value.resolvedType)
            }
            is BinaryOperation -> {
                val left = checkType(node.left)
                val right = checkType(node.right)
                checkCompatibility(node, left.resolvedType, right.resolvedType)
            }
            is Block -> checkTypes(node.statements)
            is ConditionalBranch -> TODO("ConditionalBranch")
            is AnonymousFunctionInvocation -> TODO("AnonymousFunctionInvocation")
            is InfixInvocation -> TODO("InfixInvocation")
            is LambdaDeclaration -> TODO("Lambda")
            is MemberAccessOperation -> {
                checkType(node.left)
                checkType(node.right)
            }
            is StringTemplateExpression -> TODO("StringTemplateExpression")
            is ExpressionStatement -> checkType(node.expression)
            is FunctionDeclaration -> {
                // TODO: type check all returns
                checkTypes(node.arguments)
                node.body ifNotNull { checkType(it) }
            }
            is LocalVariable -> {
                val type = node.type
                val initializer = checkType(node.initializer)
                if (initializer != null) {
                    checkCompatibility(initializer, type.resolvedType, initializer.resolvedType)
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
            is Literal -> {}
            is InvocationArgument -> {}
            // type checking of return expressions is done where they're placed
            is Return -> {}
            // it's already been "type checked" in the type resolution phase
            is FunctionInvocation -> {}
            is TypeName -> {}
        }
        return node
    }

    private fun <T : Node> checkTypes(nodes: List<T>) {
        nodes.forEach(::checkType)
    }

    private fun checkCompatibility(position: Positionable, a: TypeOrError?, b: TypeOrError?) {
        require(a is Type) { notResolved(a) }
        require(b is Type) { notResolved(b) }
        if (a isIncompatible b) {
            reportIncompatibleTypes(position, a, b)
        }
    }

    private fun notResolved(type: TypeOrError?): String = when (type) {
        null -> "Undefined type encountered. Was type resolution skipped?"
        else -> "Error type encountered. Were inputs not filtered?"
    }

    private fun reportIncompatibleTypes(
        position: Positionable,
        expected: Type,
        got: Type,
    ) {
        reportTypeError(position, formatIncompatibleTypes(expected, got))
    }

    companion object {
        fun run(unit: CompilationUnit): JukkasResult<CompilationUnit> = run(unit, unit.source)

        fun <T : Node> run(node: T, source: Source): JukkasResult<T> {
            val phase = TypeCheckingPhase(source)
            phase.checkType(node)
            return phase.reporter.toResult { node }
        }
    }
}