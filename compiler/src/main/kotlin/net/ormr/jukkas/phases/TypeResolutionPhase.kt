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
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.ast.Function
import net.ormr.jukkas.type.ErrorType
import net.ormr.jukkas.type.JvmPrimitiveType
import net.ormr.jukkas.type.ResolvedType
import net.ormr.jukkas.type.ResolvedTypeOrError
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeCache
import net.ormr.jukkas.type.TypeResolutionContext
import net.ormr.jukkas.type.UnknownType

/**
 * Performs type resolution and type inference.
 */
class TypeResolutionPhase(private val unit: CompilationUnit) : NodeVisitor<Unit> {
    private val context = object : TypeResolutionContext {
        override val cache: TypeCache
            get() = unit.types

        override fun reportSemanticError(position: Positionable, message: String) {
            unit.reportSemanticError(position, message)
        }

        override fun reportTypeError(position: Positionable, message: String) {
            unit.reportTypeError(position, message)
        }
    }

    override fun visitCompilationUnit(unit: CompilationUnit) {
        unit.children.forEach(::visit)
    }

    override fun visitArgument(argument: Argument) {
        if (argument is NamedArgument) {
            val type = argument.type
            argument.type = resolveType(type)
        }
    }

    override fun visitAssignmentOperation(operation: AssignmentOperation) {
        visit(operation.left)
        operation.type = visitAndGetType(operation.value)
    }

    // TODO: resolve these types as if a function once we operator overloading put in
    override fun visitBinaryOperation(operation: BinaryOperation) {
        visit(operation.left)
        visit(operation.right)
        // TODO: set 'operation.type' once we figure out a better way of handling this
    }

    override fun visitBlock(block: Block) {
        block.statements.forEach(::visit)
    }

    override fun visitMemberAccessOperation(operation: MemberAccessOperation) {
        visit(operation.left)
        operation.type = visitAndGetType(operation.right)
    }

    override fun visitConditionalBranch(conditional: ConditionalBranch) {
        // TODO: do we want to only resolve and infer types for a conditional branch if it's actually used as
        //       an expression? could potentially improve performance, as type inference and resolution can be
        //       relatively costly.
        val (condition, thenBranch, elseBranch) = conditional
        visit(condition)
        visit(thenBranch)
        visit(elseBranch)
        // TODO: resolve type ourselves
        conditional.type = TypeInference.inferType(conditional)
    }

    override fun visitExpressionStatement(statement: ExpressionStatement) {
        visit(statement.expression)
    }

    override fun visitFunction(function: Function) {
        function.arguments.forEach(::visit)
        visit(function.body)
        // TODO: actually infer the type
        function.type = resolveTypeIfNeeded(function.type) { JvmPrimitiveType.VOID }
    }

    override fun visitIdentifierReference(reference: DefinitionReference) {
        val definition = findDefinition(reference) ?: return
        visit(definition.asNode())
    }

    override fun visitInvocation(invocation: Invocation) {
        // TODO: set the type of this at some point
        when (invocation) {
            is FunctionInvocation -> {
                val (left, arguments) = invocation
                visit(left)
                arguments.forEach(::visit)
                when (left) {
                    is DefinitionReference -> {
                        val definition = findDefinition(left) ?: return
                        TODO("standalone definition")
                    }
                    is MemberAccessOperation -> {
                        // TODO: better variable names
                        // TODO: this code is very hacky and needs to be severely improved
                        val priorType = visitAndGetType(left.left)
                        val right = left.right
                        if (priorType !is ResolvedType) return
                        require(right is DefinitionReference) { "FIXME" }
                        val name = right.name
                        val types = arguments.map { it.type as ResolvedType }
                        val method = priorType.findMethod(name, types)
                        if (method != null) {
                            invocation.member = method
                        } else {
                            val signature = "$name(${types.joinToString { it.internalName }})"
                            reportSemanticError(
                                invocation,
                                "Could not find a function with signature: $signature",
                            )
                        }
                    }
                    else -> reportSemanticError(left, "Not invokable")
                }
            }
            is InfixInvocation -> TODO("infix invocation")
        }
    }

    override fun visitInvocationArgument(argument: InvocationArgument) {
        argument.type = visitAndGetType(argument.value)
    }

    override fun visitProperty(property: Property) {
        property.type = when (val type = property.type) {
            is UnknownType -> when (val initializer = property.initializer) {
                null -> errorType(property, "Could not infer variable type, please specify it explicitly.")
                else -> visitAndGetType(initializer)
            }
            else -> resolveType(type)
        }
    }

    override fun visitReturn(expr: Return) {
        val value = expr.value
        expr.type = when (value) {
            // TODO: return JukkasType.UNIT
            null -> JvmPrimitiveType.VOID
            else -> visitAndGetType(value)
        }
    }

    override fun visitVariable(variable: Variable) {
        variable.type = resolveTypeIfNeeded(variable.type) {
            when (val initializer = variable.initializer) {
                null -> errorType(variable, "Could not infer variable type, please specify it explicitly.")
                else -> visitAndGetType(initializer)
            }
        }
    }

    override fun visitStringTemplateExpression(variable: StringTemplateExpression) {
        TODO("Not yet implemented")
    }

    // nodes with nothing to resolve / infer
    override fun visitImport(import: Import) {}

    override fun visitImportEntry(entry: ImportEntry) {}

    override fun visitLiteral(literal: Literal) {}

    override fun visitPattern(pattern: Pattern) {}

    // misc
    private fun findDefinition(reference: DefinitionReference): Definition? {
        // TODO: should we handle this potential error in a better manner?
        val parent = reference.parent ?: error("No parent found for $reference")
        return reference.find(parent.closestTable) ?: run {
            reportSemanticError(reference, "Unresolved reference: ${reference.name}")
            null
        }
    }

    private fun resolveType(type: Type): ResolvedTypeOrError {
        require(type !is UnknownType) { "Can't resolve an UnknownType. Was type inference skipped?" }
        return type.resolve(context)
    }

    private fun errorType(position: Positionable, message: String): ErrorType {
        unit.reportTypeError(position, message)
        return ErrorType(message)
    }

    private fun reportSemanticError(position: Positionable, message: String) {
        unit.reportSemanticError(position, message)
    }

    // TODO: better name
    private inline fun resolveTypeIfNeeded(type: Type, ifUnknown: () -> Type): Type = when (type) {
        is UnknownType -> ifUnknown()
        is ResolvedType -> type
        else -> resolveType(type)
    }

    // TODO: better name
    private fun visitAndGetType(expr: Expression): Type {
        visit(expr)
        return when (val type = expr.type) {
            is UnknownType -> errorType(expr, "Failed to resolve type.")
            else -> type
        }
    }
}