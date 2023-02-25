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

package net.ormr.jukkas.phase

import net.ormr.asmkt.BytecodeClass
import net.ormr.asmkt.BytecodeMethod
import net.ormr.asmkt.BytecodeVersion
import net.ormr.asmkt.Modifiers
import net.ormr.asmkt.defineClass
import net.ormr.asmkt.defineMethod
import net.ormr.asmkt.types.MethodType
import net.ormr.asmkt.types.ReferenceType
import net.ormr.jukkas.JukkasResult
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.phases.CompilerPhase
import net.ormr.jukkas.type.AsmFieldType
import net.ormr.jukkas.type.AsmMethodType
import net.ormr.jukkas.type.AsmReferenceType
import net.ormr.jukkas.type.ContainerType
import net.ormr.jukkas.type.JvmPrimitiveType
import net.ormr.jukkas.type.JvmType
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.member.JvmTypeMember
import net.ormr.jukkas.type.member.TypeMember
import net.ormr.krautils.lang.ifNotNull
import kotlin.io.path.name
import kotlin.io.path.pathString

class BytecodeGenerationPhase private constructor(source: Source) : CompilerPhase(source) {
    private val results = mutableListOf<BytecodeResult>()

    fun generateBytecode(unit: CompilationUnit): List<BytecodeResult> {
        val sourceFile = (unit.source as? Source.File)?.path?.pathString
        val className = when (unit.source) {
            is Source.File -> unit.source.path.name.substringBeforeLast('.')
            is Source.Repl -> "repl"
            is Source.Text -> "text"
        }
        // TODO: infer package path
        val refType = ReferenceType.fromInternal(className)
        val clz = defineClass(refType, BytecodeVersion.JAVA_17, sourceFile = sourceFile) {
            unit.children.forEach { generateTopLevel(it) }
        }
        addResult(clz)
        return results
    }

    private fun BytecodeClass.generateTopLevel(node: TopLevel) {
        when (node) {
            is FunctionDeclaration -> generateFunction(node, Modifiers.STATIC)
            is Property -> TODO()
            is Import -> unreachable<Import>()
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun BytecodeClass.generateFunction(
        node: FunctionDeclaration,
        extraModifiers: Int,
    ) = defineMethod(node.name, Modifiers.PUBLIC or extraModifiers, getMethodType(node)) {
        val start = mark()
        node.body?.statements?.forEach { generateLocal(it) }
        val end = mark()
        // TODO: define parameters and local variables
    }

    private fun BytecodeMethod.generateLocal(node: Statement) {
        when (node) {
            is AssignmentOperation -> TODO("AssignmentOperation")
            is BinaryOperation -> TODO("BinaryOperation")
            is Block -> TODO("Block")
            is ConditionalBranch -> TODO("ConditionalBranch")
            is DefinitionReference -> {
                // TODO: invoking a property (non-field) could have side effects, so we probably want
                //       to generate code for calling a definition reference if it's a property too
                val definition = findDefinition(node)
                if (definition is LocalVariable) {
                    require(definition.index != -1) { "Index not set for <$definition>" }
                    val type = getAsmType(definition)
                    loadLocal(definition.index, type)
                }
            }
            is Invocation -> when (node) {
                is AnonymousFunctionInvocation -> TODO("AnonymousFunctionInvocation")
                // TODO: this should maybe be an unreachable in the future
                //       we should at an earlier phase resolve all standalone function
                //       invocations to something like a MemberAccessOperation
                is FunctionInvocation -> generateInvocation(node)
                is InfixInvocation -> TODO("InfixInvocation")
            }
            // TODO: handle potential name
            is InvocationArgument -> generateLocal(node.value)
            is LambdaDeclaration -> TODO("LambdaDeclaration")
            is Literal -> when (node) {
                is BooleanLiteral -> pushBoolean(node.value)
                is Int32Literal -> pushInt(node.value)
                is StringLiteral -> pushString(node.value)
            }
            is MemberAccessOperation -> {
                require(!node.isSafe) { "Safe access is not yet supported" }
                generateLocal(node.left)
                when (val right = node.right) {
                    // *.bar
                    is DefinitionReference -> {
                        val member = requireNotNull(right.member) { "Missing member on <$right>" }
                        val name = member.name
                        val ownerType = getAsmReferenceType(member.declaringType)
                        val fieldType = getAsmType(member)
                        if (member.isStatic) {
                            getStaticField(ownerType, name, fieldType)
                        } else {
                            getField(ownerType, name, fieldType)
                        }
                    }
                    // *.bar()
                    is FunctionInvocation -> generateInvocation(right)
                }
            }
            is Return -> {
                node.value ifNotNull { generateLocal(it) }
                returnValue()
            }
            is StringTemplateExpression -> TODO("StringTemplateExpression")
            is ExpressionStatement -> generateLocal(node.expression)
            is FunctionDeclaration -> TODO("FunctionDeclaration")
            is LocalVariable -> TODO("LocalVariable")
        }
    }

    private fun BytecodeMethod.generateInvocation(node: FunctionInvocation) {
        val member = requireNotNull(node.member) { "Function invocation missing 'member'" }
        require(member is TypeMember.Function) { "Invoking of constructors is not implemented yet" }
        node.arguments.forEach { generateLocal(it) }
        val ownerType = getAsmReferenceType(member)
        val memberType = getAsmMethodType(member)
        // TODO: handle invokeSpecial for invoking the super type
        when {
            member.isStatic -> invokeStatic(ownerType, node.name, memberType)
            member.declaringType.isInterface -> invokeInterface(ownerType, node.name, memberType)
            else -> invokeVirtual(ownerType, node.name, memberType)
        }

        // pop any values left on the stack
        if (node.isExpressionStatement && node.resolvedType != JvmPrimitiveType.VOID) {
            // TODO: should we pop2 in case type is long or double?
            pop()
        }
    }

    private fun getType(node: HasType): Type {
        val resolvedType = when (node) {
            is Definition -> node.findTypeName().resolvedType
            is AbstractExpression -> node.resolvedType
        }
        val type = requireNotNull(resolvedType) { "Type for node <$node> is not missing. Was type resolution skipped?" }
        check(type is Type) { "Type for <$node> is ErrorType. Did a phase leak through?" }
        return type
    }

    private fun getAsmType(node: HasType): AsmFieldType = getAsmType(getType(node))

    private fun getAsmType(member: TypeMember.HasType): AsmFieldType = getAsmType(member.findType())

    private fun getAsmType(type: Type): AsmFieldType = when (type) {
        is ContainerType -> when (type) {
            is JvmType -> type.toAsmType()
            else -> createAsmType(type)
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun createAsmType(type: Type): AsmReferenceType = TODO("createAsmType")

    private fun getAsmReferenceType(member: TypeMember): AsmReferenceType {
        val asmType = getAsmType(member.declaringType)
        check(asmType is ReferenceType) {
            "Member <$member> does not belong to a reference type. This should never happen."
        }
        return asmType
    }

    private fun getAsmReferenceType(type: Type): AsmReferenceType {
        val asmType = getAsmType(type)
        check(asmType is ReferenceType) { "Type <$type> is not a reference type" }
        return asmType
    }

    private fun getAsmReferenceType(node: HasType): AsmReferenceType = getAsmReferenceType(getType(node))

    private fun getAsmMethodType(member: TypeMember.Executable): AsmMethodType = when (member) {
        is JvmTypeMember.Executable -> member.toAsmMethodType()
        else -> TODO("createAsmMethodType")
    }

    private fun addResult(clz: BytecodeClass) {
        results += BytecodeResult(clz.internalName, clz.toByteArray())
    }

    private fun Type.toJvmDescriptor(): String = when (this) {
        is ContainerType -> when (this) {
            is JvmType -> toJvmDescriptor()
            else -> TODO("Attempt to create a jvm descriptor")
        }
    }

    private fun getMethodType(function: FunctionDeclaration): MethodType {
        val descriptor = buildString {
            function.arguments.joinTo(this, "", "(", ")") { getType(it).toJvmDescriptor() }
            append(getType(function).toJvmDescriptor())
        }
        return MethodType.fromDescriptor(descriptor)
    }

    class BytecodeResult(val name: String, val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean = when {
            this === other -> true
            other !is BytecodeResult -> false
            name != other.name -> false
            !bytes.contentEquals(other.bytes) -> false
            else -> true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }

        override fun toString(): String = "BytecodeResult(name='$name', bytes=[...])"

        operator fun component1(): String = name

        operator fun component2(): ByteArray = bytes
    }

    companion object {
        fun run(unit: CompilationUnit): JukkasResult<List<BytecodeResult>> {
            val phase = BytecodeGenerationPhase(unit.source)
            return phase.reporter.toResult { phase.generateBytecode(unit) }
        }
    }
}