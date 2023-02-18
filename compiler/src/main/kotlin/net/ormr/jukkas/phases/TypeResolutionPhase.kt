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
import net.ormr.jukkas.type.ErrorType
import net.ormr.jukkas.type.JukkasType
import net.ormr.jukkas.type.JvmPrimitiveType
import net.ormr.jukkas.type.ResolvedType
import net.ormr.jukkas.type.ResolvedTypeOrError
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeCache
import net.ormr.jukkas.type.TypeResolutionContext
import net.ormr.jukkas.type.UnknownType

/**
 * Performs type resolution and type inference, and some light type checking.
 */
class TypeResolutionPhase private constructor(source: Source, private val types: TypeCache) : CompilerPhase(source) {
    private val context = object : TypeResolutionContext {
        override val cache: TypeCache
            get() = types

        override fun reportSemanticError(position: Positionable, message: String) {
            this@TypeResolutionPhase.reportSemanticError(position, message)
        }

        override fun reportTypeError(position: Positionable, message: String) {
            this@TypeResolutionPhase.reportTypeError(position, message)
        }
    }

    private fun check(node: Node?) {
        when (node) {
            is HasType -> resolve(node)
            is Import -> {
                val path = node.path.value
                for (entry in node.entries) {
                    val (name, alias) = entry
                    val type = ResolvedType.find(Type.buildJavaName(path, name))
                    if (type != null) {
                        types.define(entry, type, alias)
                    } else {
                        reportSemanticError(entry, "Unable to find ${path}/${name}")
                    }
                }
            }
            is ImportEntry -> unreachable<ImportEntry>()
            is Pattern -> TODO("Pattern")
            is PatternArgument -> TODO("PatternArgument")
            is ExpressionStatement -> check(node.expression)
            is StringTemplatePart -> TODO("StringTemplatePart")
            is CompilationUnit -> {
                node.imports.forEach(::check)
                node.children.forEach(::check)
            }
            null -> {
                // do nothing
            }
        }
    }

    @JvmName("resolveNullable")
    private fun resolve(node: HasType?): ResolvedTypeOrError? = node?.let(::resolve)

    private fun resolve(node: HasType): ResolvedTypeOrError = node resolveIfNeeded {
        when (this) {
            is Definition -> resolveDefinition(this)
            is AssignmentOperation -> {
                check(value)
                resolve(left)
            }
            is BinaryOperation -> {
                // TODO: resolve these as functions when we have operator overloading setup
                val left = resolve(left)
                val right = resolve(right)
                if (left isIncompatible right) incompatibleTypes(this, left, right) else right
            }
            is Block -> {
                // TODO: infer type of block
                statements.forEach(::check)
                JukkasType.UNIT
            }
            is ConditionalBranch -> {
                // TODO: infer type of conditional branch
                check(condition)
                check(thenBranch)
                check(elseBranch)
                JukkasType.UNIT
            }
            is DefinitionReference -> when (val definition = findDefinition(this)) {
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
            is Invocation -> resolveInvocation(this)
            is InvocationArgument -> resolve(value)
            is Literal -> type
            is MemberAccessOperation -> ifResolved(resolve(left)) { targetType ->
                when (val right = right) {
                    is FunctionInvocation -> {
                        val name = right.name
                        val arguments = right.arguments.map { resolve(it) }
                        // TODO: check if we can even access the method
                        when (val method = targetType.findMethod(name, arguments)) {
                            null -> {
                                val signature = "$name(${arguments.joinToString { it.internalName }})"
                                semanticErrorType(right, "No function found with signature <$signature>")
                            }
                            else -> {
                                member = method
                                right.member = method
                                method.returnType
                            }
                        }.updateNode(right)
                    }
                    is DefinitionReference -> {
                        val name = right.name
                        // TODO: check if we can even access the field
                        when (val field = targetType.findField(name)) {
                            null -> unresolvedReference(right, name)
                            else -> {
                                member = field
                                right.member = field
                                field.type
                            }
                        }.updateNode(right)
                    }
                    else -> errorType(right, "Expected invocation or reference")
                }
            }
            is Return -> resolveIfPossible(value) { JvmPrimitiveType.VOID } // TODO: JukkasType.UNIT
            is StringTemplateExpression -> type
        }
    }

    private fun resolveDefinition(node: Definition): ResolvedTypeOrError = node resolveIfNeeded {
        when (this) {
            is Invokable<*> -> when (this) {
                is FunctionDeclaration -> {
                    arguments.forEach(::check)
                    check(body)
                    // TODO: actually infer the type
                    resolveTypeIfNeeded(type) { JvmPrimitiveType.VOID }
                }
                is LambdaDeclaration -> TODO("Lambda")
            }
            is NamedArgument -> resolveTypeIfNeeded(type) { unresolvedType(this) }
            is Variable -> when (this) {
                is Property -> TODO("Property")
                is LocalVariable -> resolveTypeIfNeeded(type) {
                    resolveIfPossible(initializer) {
                        errorType(this, "Could not infer variable type, please specify it explicitly.")
                    }
                }
            }
        }
    }

    private fun resolveInvocation(node: Invocation): ResolvedTypeOrError = node resolveIfNeeded {
        when (this) {
            is AnonymousFunctionInvocation -> TODO("AnonymousFunctionInvocation")
            is FunctionInvocation -> TODO("FunctionInvocation")
            is InfixInvocation -> TODO("InfixInvocation")
        }
    }

    private inline infix fun <reified T : HasType> T.resolveIfNeeded(
        crossinline action: T.() -> ResolvedTypeOrError,
    ): ResolvedTypeOrError = when (val type = type) {
        !is ResolvedTypeOrError -> {
            val resolvedType = action()
            if (this is HasMutableType) {
                this.type = resolvedType
            }
            resolvedType
        }
        else -> type
    }

    private fun ResolvedTypeOrError.updateNode(node: Node): ResolvedTypeOrError = apply {
        if (node is HasMutableType) {
            node.type = this
        }
    }

    private inline fun ifResolved(
        type: ResolvedTypeOrError,
        action: (ResolvedType) -> ResolvedTypeOrError,
    ): ResolvedTypeOrError = when (type) {
        is ResolvedType -> action(type)
        is ErrorType -> type
    }

    private inline fun resolveIfPossible(
        node: HasType?,
        fallback: () -> ResolvedTypeOrError,
    ): ResolvedTypeOrError = when (node) {
        null -> fallback()
        else -> resolve(node)
    }

    private inline fun resolveTypeIfNeeded(
        type: Type,
        ifUnknown: () -> ResolvedTypeOrError,
    ): ResolvedTypeOrError = when (type) {
        is UnknownType -> ifUnknown()
        is ResolvedTypeOrError -> type
        else -> resolveType(type)
    }

    private fun resolveType(type: Type): ResolvedTypeOrError = type.resolve(context)

    private fun unresolvedType(position: Positionable): ErrorType =
        errorType(position, "Unresolved type")

    private fun unresolvedReference(position: Positionable, name: String): ErrorType =
        semanticErrorType(position, "Unresolved reference: $name")

    private fun semanticErrorType(position: Positionable, message: String): ErrorType {
        reportSemanticError(position, message)
        return ErrorType(message)
    }

    private fun errorType(position: Positionable, message: String): ErrorType {
        reportTypeError(position, message)
        return ErrorType(message)
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
        fun run(unit: CompilationUnit): JukkasResult<CompilationUnit> = run(unit, unit.source, unit.types)

        fun <T : Node> run(node: T, source: Source, types: TypeCache): JukkasResult<T> {
            val phase = TypeResolutionPhase(source, types)
            phase.check(node)
            return phase.reporter.toResult { node }
        }
    }
}