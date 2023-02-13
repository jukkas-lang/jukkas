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

package net.ormr.jukkas.cli

import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.italic
import com.github.ajalt.mordant.terminal.Terminal
import net.ormr.jukkas.JukkasResult
import net.ormr.jukkas.Source
import net.ormr.jukkas.endPoint
import net.ormr.jukkas.groupedMessages
import net.ormr.jukkas.reporter.Message
import net.ormr.jukkas.startPoint
import kotlin.system.exitProcess

class CliErrorReporter {
    private val fileSources = hashMapOf<Source, List<String>>()

    fun printErrors(terminal: Terminal, failure: JukkasResult.Failure): Nothing {
        val errors = failure.messages
        terminal.println(bold("${errors.size} ${red("errors")}"))
        failure.groupedMessages.forEach { (file, messages) ->
            terminal.println(bold("${file.description}:"))
            messages.forEach {
                if (it is Message.Error) {
                    terminal.printError(file, it)
                }
            }
            terminal.println()
        }
        exitProcess(1)
    }

    private fun Terminal.printError(source: Source, message: Message.Error) {
        val (line, column) = message.position.startPoint
        println("${bold("${italic("[${line + 1}:${column + 1}]")}: ${red("error")}")}: ${message.message}")
        printSource(source, message)
    }

    // TODO: if token is end of line, we want to put the arrow after the faulty value
    private fun Terminal.printSource(source: Source, message: Message) {
        val sourceText = fileSources.getOrPut(source) { source.reader().readLines() }
        // TODO: we need to implement our own way of converting the indices we have to line:column
        val start = message.position.startPoint
        val end = message.position.endPoint
        // TODO: handle multi line stuff
        if (start.line != end.line) {
            println(gray(message.message))
            return
        }
        // TODO: trim away useless leading indent from source
        val content = sourceText[start.line]
        print(INDENT)
        println(content)
        print(INDENT)
        print(" ".repeat(start.column))
        println(bold(cyan("^".repeat((end.column - start.column) + 1))))
    }

    private companion object {
        private const val INDENT = "    "
    }
}