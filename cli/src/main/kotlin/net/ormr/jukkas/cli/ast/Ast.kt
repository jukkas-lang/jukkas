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

package net.ormr.jukkas.cli.ast

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.ormr.jukkas.*
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.cli.CliErrorReporter
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.phases.TypeCheckingPhase
import net.ormr.jukkas.phases.TypeResolutionPhase
import net.ormr.jukkas.type.JvmTypeResolver
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.member.JukkasMember
import net.ormr.jukkas.type.member.JvmMember
import net.ormr.jukkas.type.member.TypeMember
import net.ormr.krautils.reflection.isAbstract
import net.ormr.krautils.reflection.isFinal
import net.ormr.krautils.reflection.isInterface
import net.ormr.krautils.reflection.isNative
import net.ormr.krautils.reflection.isPrivate
import net.ormr.krautils.reflection.isProtected
import net.ormr.krautils.reflection.isPublic
import net.ormr.krautils.reflection.isStatic
import net.ormr.krautils.reflection.isStrict
import net.ormr.krautils.reflection.isSynchronized
import net.ormr.krautils.reflection.isTransient
import net.ormr.krautils.reflection.isVolatile
import kotlin.io.path.name
import java.lang.reflect.Member as JavaMember

class Ast : CliktCommand(help = "Ast stuff", printHelpOnEmptyArgs = true) {
    private val reporter = CliErrorReporter()
    private val file by option("-f", "--file")
        .help("The file to read input from")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()
    private val includePosition by option("--position")
        .help("Whether the JSON should include positions for the AST nodes")
        .flag("--no-position")

    private val json = Json {
        prettyPrint = true
    }

    override fun run() {
        val terminal = currentContext.terminal
        val context = buildCompilationContext {
            types {
                resolver(JvmTypeResolver)
            }
        }
        val unit = JukkasParser
            .parseFile(file)
            .flatMap { TypeResolutionPhase.run(it.value, context) }
            .flatMap { TypeCheckingPhase.run(it.value) }
            .getOrElse { reporter.printErrors(terminal, it) }
        terminal.println(json.encodeToString(toJson(unit)))
    }

    private fun jsonPosition(position: Position): JsonObject = when (position) {
        is Point -> buildJsonObject {
            put("line", position.line)
            put("column", position.column)
        }
        is Span -> buildJsonObject {
            put("start", jsonPosition(position.start))
            put("end", jsonPosition(position.end))
        }
    }

    @JvmName("nullableJsonPosition")
    private fun jsonPosition(position: Position?): JsonElement = position?.let(::jsonPosition) ?: JsonNull

    private fun JsonObjectBuilder.includeNode(node: Node) {
        put("nodeType", node.javaClass.simpleName)
        if (node is Expression) {
            put("type", toJson(node.type))
        } else if (node is Definition) {
            put("type", toJson(node.type))
        }
        if (includePosition) put("position", jsonPosition(node.position))
        // TODO: add table keys if node is TableContainer
    }

    private fun JsonObjectBuilder.putNodeList(name: String, nodes: Iterable<Node>) {
        putJsonArray(name) {
            nodes.forEach { add(toJson(it)) }
        }
    }

    // TODO: better dumping of members
    private fun toJson(member: TypeMember?): JsonElement = when (member) {
        is JvmMember -> when (member) {
            is JvmMember.Constructor -> JsonPrimitive(member.constructor.toString())
            is JvmMember.Field -> JsonPrimitive(member.field.toString())
            is JvmMember.Method -> JsonPrimitive(member.method.toString())
        }
        is JukkasMember -> TODO("JukkasMember")
        null -> JsonNull
    }

    @Suppress("UnusedPrivateMember")
    private fun buildModifiers(member: JavaMember): JsonArray = buildJsonArray {
        if (member.isPublic) add("public")
        if (member.isPrivate) add("private")
        if (member.isProtected) add("protected")
        if (member.isInterface) add("interface")
        if (member.isAbstract) add("abstract")
        if (member.isFinal) add("final")
        if (member.isStatic) add("static")
        if (member.isSynchronized) add("synchronized")
        if (member.isVolatile) add("volatile")
        if (member.isTransient) add("transient")
        if (member.isNative) add("native")
        if (member.isStrict) add("strict")
    }

    private fun toJson(type: Type): JsonElement = JsonPrimitive(type.internalName)

    @JvmName("nullableToJson")
    private fun toJson(node: Node?): JsonElement = node?.let(::toJson) ?: JsonNull

    // TODO: move this to the Node class itself?
    private fun toJson(node: Node): JsonElement = when (node) {
        is CompilationUnit -> buildJsonObject {
            includeNode(node)
            put("source", fileName(node.source))
            putNodeList("imports", node.imports)
            putNodeList("children", node.children)
        }
        is Import -> buildJsonObject {
            includeNode(node)
            put("path", node.path.value)
            putNodeList("entries", node.entries)
        }
        is ImportEntry -> buildJsonObject {
            includeNode(node)
            put("name", node.name)
            if (node.alias != null) put("alias", node.alias)
        }
        is Pattern -> TODO("Pattern")
        is Argument -> when (node) {
            is BasicArgument -> buildJsonObject {
                includeNode(node)
                put("name", node.name)
            }
            is DefaultArgument -> buildJsonObject {
                includeNode(node)
                put("name", node.name)
                put("default", toJson(node.default))
            }
            is PatternArgument -> TODO("PatternArgument")
        }
        is AssignmentOperation -> TODO()
        is BinaryOperation -> TODO()
        is Block -> buildJsonObject {
            includeNode(node)
            putNodeList("children", node.statements)
        }
        is ConditionalBranch -> TODO()
        is DefinitionReference -> buildJsonObject {
            includeNode(node)
            put("name", node.name)
        }
        is Invocation -> when (node) {
            is FunctionInvocation -> buildJsonObject {
                includeNode(node)
                put("name", node.name)
                putNodeList("arguments", node.arguments)
                put("member", toJson(node.member))
            }
            is AnonymousFunctionInvocation -> buildJsonObject {
                includeNode(node)
                put("target", toJson(node.target))
                putNodeList("arguments", node.arguments)
                put("member", toJson(node.member))
            }
            is InfixInvocation -> TODO()
        }
        is InvocationArgument -> buildJsonObject {
            includeNode(node)
            put("name", node.name)
            put("value", toJson(node.value))
        }
        is LambdaDeclaration -> TODO()
        is Literal -> buildJsonObject {
            includeNode(node)
            val value = when (node) {
                is BooleanLiteral -> JsonPrimitive(node.value)
                is IntLiteral -> JsonPrimitive(node.value)
                is StringLiteral -> JsonPrimitive(node.value)
                is SymbolLiteral -> TODO()
            }
            put("value", value)
        }
        is MemberAccessOperation -> buildJsonObject {
            includeNode(node)
            put("left", toJson(node.left))
            put("isSafe", node.isSafe)
            put("right", toJson(node.right))
        }
        is Return -> TODO()
        is StringTemplateExpression -> TODO()
        is ExpressionStatement -> buildJsonObject {
            includeNode(node)
            put("expression", toJson(node.expression))
        }
        is FunctionDeclaration -> buildJsonObject {
            includeNode(node)
            put("name", node.name)
            putNodeList("arguments", node.arguments)
            put("body", toJson(node.body))
        }
        is Property -> TODO()
        is LocalVariable -> TODO()
        is StringTemplatePart.ExpressionPart -> TODO()
        is StringTemplatePart.LiteralPart -> TODO()
    }

    private fun fileName(source: Source): String = when (source) {
        is Source.File -> source.path.name
        is Source.Repl, is Source.Text -> source.description
    }
}