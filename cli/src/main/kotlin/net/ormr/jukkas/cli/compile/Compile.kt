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

package net.ormr.jukkas.cli.compile

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import net.ormr.jukkas.cli.CliErrorReporter
import net.ormr.jukkas.flatMap
import net.ormr.jukkas.getOrElse
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.phases.BytecodeGenerationPhase
import net.ormr.jukkas.phases.TypeCheckingPhase
import net.ormr.jukkas.phases.TypeResolutionPhase
import net.ormr.krautils.lang.ifNotNull
import net.ormr.krautils.reflection.isStatic
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

class Compile : CliktCommand(help = "Compile stuff", printHelpOnEmptyArgs = true) {
    private val reporter = CliErrorReporter()
    private val file by option("-f", "--file")
        .help("The file to read input from")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()
    private val output by option("-o", "--output")
        .help("The directory to output the class files to")
        .path(canBeFile = false)


    // TODO: this is temporary for now
    private val runClass by option("-r", "--run")
        .help("Should the class be ran after compilation")
        .flag()

    override fun run() {
        val terminal = currentContext.terminal
        val classes = JukkasParser
            .parseFile(file)
            .flatMap { TypeResolutionPhase.run(it.value) }
            .flatMap { TypeCheckingPhase.run(it.value) }
            .flatMap { BytecodeGenerationPhase.run(it.value) }
            .getOrElse { reporter.printErrors(terminal, it) }
        output ifNotNull { output ->
            val root = output.createDirectories()
            for ((name, bytes) in classes) {
                val file = root.resolve("$name.class")
                file.writeBytes(bytes)
            }
        }
        if (runClass) {
            val (name, bytes) = classes.single()
            val loader = ByteClassLoader()
            val clz = loader.defineClass(name, bytes)
            val method = clz.methods.single { it.isStatic && it.name == "main" }
            method.invoke(null)
        }
    }

    class ByteClassLoader(
        name: String? = null,
        parent: ClassLoader = getSystemClassLoader(),
    ) : ClassLoader(name, parent) {
        fun defineClass(name: String, bytes: ByteArray): Class<*> = defineClass(name, bytes, 0, bytes.size)
    }
}