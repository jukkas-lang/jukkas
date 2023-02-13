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

import ajs.printutils.Color
import ajs.printutils.PrettyPrintTree
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.*
import net.ormr.jukkas.ast.Function
import net.ormr.jukkas.cli.CliErrorReporter
import net.ormr.jukkas.getOrElse
import net.ormr.jukkas.parser.JukkasParser
import kotlin.io.path.name

class Ast : CliktCommand(help = "Ast stuff", printHelpOnEmptyArgs = true) {
    private val reporter = CliErrorReporter()
    private val file by option("-f", "--file")
        .help("The file to read input from")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()

    override fun run() {
        val terminal = currentContext.terminal
        val unit = JukkasParser.parseFile(file).getOrElse { reporter.printErrors(terminal, it) }
        val tree = createRootTree(unit)
        val printTree = PrettyPrintTree(Tree::children, Tree::value).apply {
            setColor(Color.NONE)
            setBorder(false)
        }
        printTree.display(tree)
    }

    private fun fileName(source: Source): String = when (source) {
        is Source.File -> source.path.name
        is Source.Repl, is Source.Text -> source.description
    }

    private fun createRootTree(unit: CompilationUnit): Tree = Tree(fileName(unit.source)) {
        for (child in unit.children) {
            addChild(createTree(child))
        }
    }

    private fun createTree(node: Statement): Tree = when (node) {
        is Block -> Tree("block") {
            for (child in node.statements) addChild(createTree(child))
        }
        is Function -> Tree("fun ${node.name ?: ""}") {
            if (node.arguments.isNotEmpty()) {
                addChild("parameters") {
                    for (arg in node.arguments) addChild(createTree(arg))
                }
            }
            val body = node.body
            if (body != null) {
                addChild(createTree(body))
            }
        }
        is DefinitionReference -> Tree(node.name)
        is Return -> Tree("return") {
            val expr = node.value
            if (expr != null) addChild(createTree(expr))
        }
        is ExpressionStatement -> createTree(node.expression)
        is Argument -> when (node) {
            is BasicArgument -> Tree(node.name)
            is DefaultArgument -> Tree(node.name) {
                addChild(createTree(node.default))
            }
            is PatternArgument -> TODO("pattern argument")
        }
        is Import -> TODO()
        is Property -> TODO()
        is Variable -> TODO()
        is Literal -> when (node) {
            is BooleanLiteral -> Tree(node.value.toString())
            is SymbolLiteral -> Tree("'${node.text}")
            else -> TODO("")
        }
        else -> TODO("${node.javaClass}")
    }

    private inline fun Tree(value: String, block: Tree.() -> Unit): Tree = Tree(value).apply(block)

    private class Tree(val value: String, val children: MutableList<Tree> = mutableListOf()) {
        fun addChild(child: Tree): Tree {
            children += child
            return child
        }

        inline fun addChild(value: String, block: Tree.() -> Unit = {}) {
            addChild(Tree(value).apply(block))
        }

        override fun toString(): String = value
    }
}