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
import net.ormr.jukkas.type.AsmReferenceType
import net.ormr.jukkas.type.JvmType
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
            // TODO: a singular definition reference by itself is a bit sus
            is DefinitionReference -> {
                require(!node.isStaticReference) { "Static references should be handled elsewhere" }
                // TODO: support references to properties when they're implemented
                val member = requireNotNull(node.member) { "Non static reference has no member <$node>" }
                val ownerType = getAsmReferenceType(member)
                getField(ownerType, member.name, (member.type as JvmType).toAsmType())
            }
            is Invocation -> when (node) {
                is AnonymousFunctionInvocation -> TODO("AnonymousFunctionInvocation")
                is FunctionInvocation -> {
                    val member = requireNotNull(node.member) { "Function invocation missing 'member'" }
                    require(member is TypeMember.Method) { "Invoking of constructors is not implemented yet" }
                    node.arguments.forEach { generateLocal(it) }
                    val ownerType = getAsmReferenceType(member)
                    val memberType = member.toAsmType()
                    // TODO: handle invokeSpecial for invoking the super type
                    when {
                        member.isStatic -> invokeStatic(ownerType, node.name, memberType)
                        member.declaringType.isInterface -> invokeInterface(ownerType, node.name, memberType)
                        else -> invokeVirtual(ownerType, node.name, memberType)
                    }
                }
                is InfixInvocation -> TODO("InfixInvocation")
            }
            // TODO: handle potential name
            is InvocationArgument -> generateLocal(node.value)
            is LambdaDeclaration -> TODO("LambdaDeclaration")
            is Literal -> when (node) {
                is BooleanLiteral -> pushBoolean(node.value)
                is IntLiteral -> pushInt(node.value)
                is StringLiteral -> pushString(node.value)
                is SymbolLiteral -> TODO("SymbolLiteral")
            }
            is MemberAccessOperation -> {
                require(!node.isSafe) { "Safe access is not yet supported" }
                val (left, right) = node
                // TODO: this looks ugly, we should have separate `DefinitionReference` types to make
                //       something like this cleaner to handle
                if (left is DefinitionReference && left.isStaticReference) {
                    val ownerType = getAsmReferenceType(left.type as JvmType)
                    if (right is DefinitionReference) {
                        val fieldType = (right.type as JvmType).toAsmType()
                        getStaticField(ownerType, right.name, fieldType)
                    } else {
                        generateLocal(right)
                    }
                } else {
                    generateLocal(left)
                    generateLocal(right)
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

    private fun getAsmReferenceType(member: TypeMember): AsmReferenceType {
        val asmType = (member.declaringType as JvmType).toAsmType()
        check(asmType is ReferenceType) {
            "Member <$member> does not belong to a reference type. This should never happen."
        }
        return asmType
    }

    private fun getAsmReferenceType(type: JvmType): AsmReferenceType {
        val asmType = type.toAsmType()
        check(asmType is ReferenceType) { "Type <$type> is not a reference type" }
        return asmType
    }

    private fun addResult(clz: BytecodeClass) {
        results += BytecodeResult(clz.internalName, clz.toByteArray())
    }

    private fun getMethodType(function: FunctionDeclaration): MethodType {
        val descriptor = buildString {
            function.arguments.joinTo(this, "", "(", ")") { (it.type as JvmType).toJvmDescriptor() }
            append((function.type as JvmType).toJvmDescriptor())
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