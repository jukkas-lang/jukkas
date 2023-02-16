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
import net.ormr.jukkas.ast.Lambda
import net.ormr.jukkas.type.ErrorType
import net.ormr.jukkas.type.ResolvedTypeOrError
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeCache
import net.ormr.jukkas.type.TypeName
import net.ormr.jukkas.type.TypeResolutionContext
import net.ormr.jukkas.type.UnknownType

/**
 * Performs type checking.
 *
 * Note that this needs to be run *after* [TypeResolutionPhase], or this phase will most likely fail fatally, as it
 * expects that types have already been resolved.
 */
internal class TypeCheckingPhase(private val unit: CompilationUnit) : NodeVisitor<Unit> {
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

        if (argument is DefaultArgument) {
            val (_, argumentType, default) = argument
            val defaultType = visitAndGetType(default)
            checkCompatibility(default, argumentType, defaultType)
        }
    }

    override fun visitAssignmentOperation(operation: AssignmentOperation) {
        val (left, _, value) = operation
        val leftType = visitAndGetType(left)
        val valueType = visitAndGetType(value)
        checkCompatibility(value, leftType, valueType)
    }

    override fun visitBinaryOperation(operation: BinaryOperation) {
        val (left, _, right) = operation
        val leftType = visitAndGetType(left)
        val rightType = visitAndGetType(right)
        // TODO: for now we report the error being the right side, but it might make more sense to report
        //       the operator as the error point once we have operator overloading, as then a type mismatch
        //       is just a failure to find a matching function
        checkCompatibility(right, leftType, rightType)
    }

    override fun visitBlock(block: Block) {
        block.statements.forEach(::visit)
        TODO("Not yet implemented")
    }

    override fun visitMemberAccessOperation(operation: MemberAccessOperation) {
        // TODO: do we want to report error or warning if it is safe access but left isn't nullable?
        visit(operation.left)
        val rightType = visitAndGetType(operation.right)
        operation.type = rightType
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

    override fun visitLambda(lambda: Lambda) {
        TODO("Not yet implemented")
    }

    override fun visitFunction(function: Function) {
        TODO("Not yet implemented")
    }

    override fun visitIdentifierReference(reference: DefinitionReference) {
        // TODO: should we handle this potential error in a better manner?
        val parent = reference.parent ?: error("No parent found for $reference")
        val definition = reference.find(parent.closestTable) ?: run {
            reportSemanticError(reference, "Unresolved reference: ${reference.name}")
            return
        }
        visit(definition)
    }

    override fun visitImport(import: Import) {
        // nothing to type check or resolve
    }

    override fun visitInvocation(invocation: Invocation) {
        TODO("Not yet implemented")
    }

    override fun visitInvocationArgument(argument: InvocationArgument) {
        TODO("Not yet implemented")
    }

    override fun visitLiteral(literal: Literal) {
        // nothing to type check or resolve
    }

    override fun visitPattern(pattern: Pattern) {
        TODO("Not yet implemented")
    }

    override fun visitProperty(property: Property) {
        TODO("Not yet implemented")
    }

    override fun visitReturn(expr: Return) {
        TODO("Not yet implemented")
    }

    override fun visitLocalVariable(variable: LocalVariable) {
        TODO("Not yet implemented")
    }

    override fun visitStringTemplateExpression(variable: StringTemplateExpression) {
        TODO("Not yet implemented")
    }

    override fun visitImportEntry(entry: ImportEntry) {
        TODO("Not yet implemented")
    }

    // nodes with nothing to type check or resolve

    // ...
    private fun reportSemanticError(position: Positionable, message: String) {
        unit.reportSemanticError(position, message)
    }

    private fun reportTypeError(position: Positionable, message: String) {
        unit.reportTypeError(position, message)
    }

    private fun reportIncompatibleTypes(
        position: Positionable,
        parent: Type,
        type: Type,
    ) {
        reportTypeError(position, "Expected type <${parent.internalName}> got <${type.internalName}>")
    }

    private fun checkCompatibility(
        position: Positionable,
        parent: Type,
        type: Type,
    ) {
        ifResolved(type) { a ->
            ifResolved(parent) { b ->
                if (a isIncompatible b) {
                    reportIncompatibleTypes(position, parent, type)
                }
            }
        }
    }

    // TODO: better name
    private fun visitAndGetType(expr: Expression): Type {
        visit(expr)
        return expr.type
    }

    private fun findPosition(node: Node, type: Type): Positionable = when (type) {
        is TypeName -> type
        else -> node
    }

    private fun resolveType(type: Type): ResolvedTypeOrError {
        require(type !is UnknownType) { "Can't resolve an UnknownType. Was type inference skipped?" }
        return type.resolve(context)
    }

    private inline fun ifResolved(type: Type, action: (ResolvedTypeOrError) -> Unit) {
        require(type is ResolvedTypeOrError) { "Type should be ResolvedType, was <$type>" }
        if (type !is ErrorType) action(type)
    }
}