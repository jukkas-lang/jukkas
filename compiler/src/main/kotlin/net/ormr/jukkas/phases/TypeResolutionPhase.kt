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

import net.ormr.jukkas.CompilerContext
import net.ormr.jukkas.JukkasResult
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.type.BuiltinTypes
import net.ormr.jukkas.type.ContainerType
import net.ormr.jukkas.type.ErrorType
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeCache
import net.ormr.jukkas.type.TypeOrError
import net.ormr.jukkas.type.flatMap
import net.ormr.jukkas.type.isIncompatible
import net.ormr.jukkas.type.member.TypeMember

/**
 * Performs type resolution and type inference, and some light type checking.
 */
class TypeResolutionPhase private constructor(
    source: Source,
    private val context: CompilerContext,
) : CompilerPhase(source) {
    private val types: TypeCache = TypeCache()
    private val builtinTypes: BuiltinTypes
        get() = context.builtinTypes

    private fun check(node: Node?) {
        when (node) {
            is HasType -> resolve(node)
            is Import -> resolveImports(node)
            is ImportEntry -> unreachable<ImportEntry>()
            is Pattern -> TODO("Pattern")
            is PatternArgument -> TODO("PatternArgument")
            is ExpressionStatement -> check(node.expression)
            is StringTemplatePart -> TODO("StringTemplatePart")
            is CompilationUnit -> {
                checkNodes(node.imports)
                checkNodes(node.children)
            }
            is TypeName, null -> {
                // do nothing
            }
        }
    }

    private fun resolve(node: HasType): TypeOrError = when (node) {
        is Definition -> resolveDefinition(node)
        is Expression -> inferType(node)
    }

    private fun resolveImports(node: Import) {
        val path = node.path.value
        for (entry in node.entries) {
            val (name, alias) = entry
            val type = context.resolveType(path, name)
            if (type != null) {
                defineType(entry, alias ?: name, type)
            } else {
                reportSemanticError(entry, "Unable to find $path/$name")
            }
        }
    }

    private fun defineType(positionable: Positionable, name: String, type: Type) {
        if (name !in types) {
            types.define(name, type)
        } else {
            reportSemanticError(positionable, "Import for $name already exists")
        }
    }

    private fun resolveDefinition(node: Definition): TypeOrError = node resolveIfNeeded {
        when (this) {
            is FunctionDeclaration -> {
                checkNodes(arguments)
                check(body)
                // TODO: actually infer the type
                resolveTypeNameIfNeeded(returnType) { builtinTypes.unit }
            }
            is NamedArgument -> resolveTypeName(type)
            is Variable -> when (this) {
                is Property -> TODO("Property")
                is LocalVariable -> resolveTypeNameIfNeeded(type) {
                    resolveIfPossible(initializer) {
                        errorType(this, "Could not infer variable type, please specify it explicitly.")
                    }
                }
            }
            else -> errorType(this, "Could not resolve type.")
        }
    }

    private fun formatBinaryComplexType(type: Type): String =
        "Binary operations are not supported for complex type: ${type.asString()}"

    // TODO: we should only infer types of expressions when it's *actually* needed
    private fun inferType(node: Expression): TypeOrError = node resolveIfNeeded {
        when (this) {
            is Definition -> resolveDefinition(this)
            is AssignmentOperation -> {
                check(value)
                resolve(left)
            }
            // TODO: resolve these as functions when we have operator overloading setup
            is BinaryOperation -> resolve(left).flatMap { left ->
                resolve(right).flatMap { right ->
                    when {
                        left !is ContainerType -> errorType(this.left, formatBinaryComplexType(left))
                        right !is ContainerType -> errorType(this.right, formatBinaryComplexType(right))
                        else -> if (left isIncompatible right) incompatibleTypes(this, left, right) else right
                    }
                }
            }
            is Block -> {
                checkNodes(statements)
                // TODO: infer type of block
                builtinTypes.unit
            }
            is LambdaDeclaration -> TODO()
            is ConditionalBranch -> {
                check(condition)
                check(thenBranch)
                check(elseBranch)
                // TODO: infer type of conditional branch
                builtinTypes.unit
            }
            is DefinitionReference -> resolveReference(this)
            is Invocation -> resolveInvocation(this)
            is InvocationArgument -> resolve(value)
            is Literal -> when (this) {
                is BooleanLiteral -> builtinTypes.boolean
                is IntLiteral -> builtinTypes.int32
                is StringLiteral -> builtinTypes.string
                is SymbolLiteral -> TODO()
            }
            is MemberAccessOperation -> resolveAccess(this)
            is Return -> resolveIfPossible(value) { builtinTypes.unit } // TODO: JukkasType.UNIT
            is StringTemplateExpression -> builtinTypes.string
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveAccess(node: MemberAccessOperation): TypeOrError = node resolveIfNeeded {
        resolve(left).flatMap { targetType ->
            // this is illegal for now, but should probably be legal once we add extension functions?
            if (targetType !is ContainerType) {
                return@flatMap semanticErrorType(
                    this,
                    "Member access operation is illegal on complex type: $targetType",
                )
            }
            // TODO: check if we can even access the member
            when (val right = right) {
                is FunctionInvocation -> {
                    val name = right.name
                    val params = right.arguments.map { resolve(it.value) }
                    if (params.any { it is ErrorType }) return@flatMap noSuchFunction(right, name, params)
                    when (val function = targetType.findFunction(name, params as List<Type>)) {
                        null -> noSuchFunction(right, name, params)
                        else -> useIfPublic(right, function) {
                            this.member = it
                            right.member = it
                        }
                    }.updateNode(right)
                }
                is DefinitionReference -> {
                    val name = right.name
                    when (val property = targetType.findProperty(name)) {
                        null -> unresolvedReference(right, name)
                        else -> useIfPublic(right, property) {
                            this.member = it
                            right.member = it
                        }
                    }.updateNode(right)
                }
                else -> errorType(right, "Expected invocation or reference")
            }
        }
    }

    private fun resolveReference(node: DefinitionReference): TypeOrError = node resolveIfNeeded {
        when (val definition = findDefinition(this)) {
            null -> {
                // if no direct definition could be found then we check the type cache for any matching type names
                val type = types.find(name)
                // if a matching type name was found, that means that we're accessing a static reference
                // TODO: this behavior is only true until we've implemented 'object' containers, as those
                //       look like static references, but need to be handled completely differently
                //       it might be better to have different types of `DefinitionReference` later on
                if (type != null) {
                    isStaticReference = true
                }
                type ?: unresolvedReference(this, name)
            }
            else -> resolveDefinition(definition)
        }
    }

    private fun resolveInvocation(node: Invocation): TypeOrError = node resolveIfNeeded {
        when (this) {
            is AnonymousFunctionInvocation -> TODO("AnonymousFunctionInvocation")
            is FunctionInvocation -> TODO("FunctionInvocation")
            is InfixInvocation -> TODO("InfixInvocation")
        }
    }

    private fun checkNodes(nodes: Iterable<Node>) {
        for (node in nodes) check(node)
    }

    private inline fun <T : TypeMember> useIfPublic(
        position: Positionable,
        member: T,
        action: (T) -> Unit,
    ): TypeOrError = when {
        member.isPublic -> {
            action(member)
            member.returnType
        }
        else -> semanticErrorType(position, "Can't access non public member: ${member.name}")
    }

    private inline infix fun <reified T : Definition> T.resolveIfNeeded(
        crossinline action: T.() -> TypeOrError,
    ): TypeOrError {
        val typeName = findTypeName()
        return when (val type = typeName.resolvedType) {
            null -> {
                val newType = action()
                typeName.resolvedType = newType
                newType
            }
            else -> type
        }
    }

    private inline infix fun <reified T : Expression> T.resolveIfNeeded(
        crossinline action: T.() -> TypeOrError,
    ): TypeOrError {
        return when (val type = resolvedType) {
            null -> {
                val newType = action()
                resolvedType = newType
                newType
            }
            else -> type
        }
    }

    private inline fun resolveIfPossible(
        node: HasType?,
        fallback: () -> TypeOrError,
    ): TypeOrError = when (node) {
        null -> fallback()
        else -> resolve(node)
    }

    private inline fun resolveTypeNameIfNeeded(
        typeName: TypeName,
        ifUndefined: () -> TypeOrError,
    ): TypeOrError = when (typeName) {
        is UndefinedTypeName -> ifUndefined()
        is DefinedTypeName -> when (val type = typeName.resolvedType) {
            null -> resolveTypeName(typeName)
            else -> type
        }
    }

    private fun unresolvedReference(position: Positionable, name: String): ErrorType =
        semanticErrorType(position, "Unresolved reference: $name")

    private fun semanticErrorType(position: Positionable, message: String): ErrorType {
        reportSemanticError(position, message)
        return ErrorType(message)
    }

    private fun errorType(position: Positionable, description: String): ErrorType {
        reportTypeError(position, description)
        return ErrorType(description)
    }

    private fun noSuchFunction(position: Positionable, name: String, params: List<TypeOrError>): ErrorType {
        val signature = "$name(${params.joinToString { it.asString() }})"
        return semanticErrorType(position, "No function found with signature: $signature")
    }

    private fun TypeOrError.updateNode(node: Node): TypeOrError = apply {
        if (node is HasType) {
            when (node) {
                is Definition -> node.findTypeName().resolvedType = this
                is Expression -> node.resolvedType = this
            }
        }
    }

    private fun resolveTypeName(typeName: TypeName): TypeOrError = types.findOrDefine(typeName.asString()) {
        when (typeName) {
            is BasicTypeName -> errorType(typeName, "Unknown type name '$typeName'")
            is IntersectionTypeName -> TODO("IntersectionType")
            is UnionTypeName -> TODO("UnionType")
            is UndefinedTypeName -> errorType(typeName, "Unresolved type")
        }
    }

    private fun incompatibleTypes(
        position: Positionable,
        expected: Type,
        got: Type,
    ): ErrorType = errorType(position, formatIncompatibleTypes(expected, got))

    private fun findDefinition(reference: DefinitionReference): NamedDefinition? {
        val parent = reference.parent
        if (parent == null) {
            reportSemanticError(reference, "Node has no parent")
            return null
        }
        val table = parent.getClosestTableOrNull()
        if (table == null) {
            reportSemanticError(reference, "Node is not connected to any table container")
            return null
        }
        return reference.find(table)
    }

    companion object {
        fun run(unit: CompilationUnit, compilerContext: CompilerContext): JukkasResult<CompilationUnit> =
            run(unit, unit.source, compilerContext)

        fun <T : Node> run(
            node: T,
            source: Source,
            compilerContext: CompilerContext,
        ): JukkasResult<T> {
            val phase = TypeResolutionPhase(source, compilerContext)
            phase.check(node)
            return phase.reporter.toResult { node }
        }
    }
}