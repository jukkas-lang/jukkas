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
import net.ormr.jukkas.type.ResolvedType
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeCache
import net.ormr.jukkas.type.TypeResolutionContext
import net.ormr.jukkas.type.UnknownType

internal class TypeResolutionPhase(private val unit: CompilationUnit) : NodeVisitor<Unit> {
    private val types get() = unit.types
    private val context = object : TypeResolutionContext {
        override val cache: TypeCache
            get() = unit.types

        override fun reportSemanticError(position: Positionable, message: String) {
            unit.reportSemanticError(position, message)
        }
    }

    private fun resolve(type: Type): ResolvedType {
        require(type !is UnknownType) { "Can't resolve UnknownType. Was type inference skipped?" }
        return type.resolve(context)
    }

    override fun visitCompilationUnit(unit: CompilationUnit) {
        unit.children.forEach(::visit)
    }

    override fun visitImport(import: Import) {
        TODO("import types from import")
    }

    override fun visitArgument(argument: Argument) {
        if (argument is NamedArgument) {
            val type = argument.type
            argument.type = resolve(type)
        }
    }

    override fun visitAssignmentOperation(operation: AssignmentOperation) {
        TODO("Not yet implemented")
    }

    override fun visitBinaryOperation(operation: BinaryOperation) {
        TODO("Not yet implemented")
    }

    override fun visitBlock(block: Block) {
        TODO("Not yet implemented")
    }

    override fun visitCall(call: Call) {
        TODO("Not yet implemented")
    }

    override fun visitConditionalBranch(conditional: ConditionalBranch) {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStatement(statement: ExpressionStatement) {
        visit(statement.expression)
    }

    override fun visitFunction(function: Function) {
        TODO("Not yet implemented")
    }

    override fun visitIdentifierReference(reference: DefinitionReference) {
        TODO("Not yet implemented")
    }

    override fun visitInvocation(invocation: Invocation) {
        TODO("Not yet implemented")
    }

    override fun visitInvocationArgument(argument: InvocationArgument) {
        TODO("Not yet implemented")
    }

    override fun visitLiteral(literal: Literal) {
        TODO("Not yet implemented")
    }

    override fun visitProperty(property: Property) {
        TODO("Not yet implemented")
    }

    override fun visitReturn(expr: Return) {
        TODO("Not yet implemented")
    }

    override fun visitVariable(variable: Variable) {
        TODO("Not yet implemented")
    }

    override fun visitStringTemplateExpression(variable: StringTemplateExpression) {
        TODO("Not yet implemented")
    }

    // nothing to resolve
    override fun visitPattern(pattern: Pattern) {}

    override fun visitImportEntry(entry: ImportEntry) {
        TODO("Not yet implemented")
    }
}