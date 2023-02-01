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

package net.ormr.jukkas.ast

interface NodeVisitor<T> {
    fun visit(node: Node): T = node.accept(this)
    fun visitCompilationUnit(unit: CompilationUnit): T
    fun visitArgument(argument: Argument): T
    fun visitAssignmentOperation(operation: AssignmentOperation): T
    fun visitBinaryOperation(operation: BinaryOperation): T
    fun visitBlock(block: Block): T
    fun visitCall(call: Call): T
    fun visitConditionalBranch(conditional: ConditionalBranch): T
    fun visitExpressionStatement(statement: ExpressionStatement): T
    fun visitFunction(function: Function): T
    fun visitIdentifierReference(reference: DefinitionReference): T
    fun visitImport(import: Import): T
    fun visitInvocation(invocation: Invocation): T
    fun visitInvocationArgument(argument: InvocationArgument): T
    fun visitLiteral(literal: Literal): T
    fun visitPattern(pattern: Pattern): T
    fun visitProperty(property: Property): T
    fun visitReturn(expr: Return): T
    fun visitVariable(variable: Variable): T
    fun visitStringTemplateExpression(variable: StringTemplateExpression): T
    fun visitExpression(expression: Expression): T = when (expression) {
        is Literal -> visitLiteral(expression)
        is BinaryOperation -> visitBinaryOperation(expression)
        is Invocation -> visitInvocation(expression)
        is Call -> visitCall(expression)
        is Return -> visitReturn(expression)
        is ConditionalBranch -> visitConditionalBranch(expression)
        is DefinitionReference -> visitIdentifierReference(expression)
        is StringTemplateExpression -> visitStringTemplateExpression(expression)
        else -> error("Can not visit <${expression}> as an expression.")
    }
}