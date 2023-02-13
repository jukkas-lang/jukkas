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

package net.ormr.jukkas

import java.io.Reader
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.reader

sealed class Source {
    abstract val description: String

    abstract fun readContent(): String

    open fun reader(): Reader = readContent().reader()

    final override fun toString(): String = description

    class Text(private val content: String) : Source() {
        override val description: String
            get() = "<text>"

        override fun readContent(): String = content
    }

    data class File(val path: Path) : Source() {
        override val description: String
            get() = path.pathString

        override fun readContent(): String = path.readText()

        override fun reader(): Reader = path.reader()
    }

    class Repl(private val content: String) : Source() {
        override val description: String
            get() = "<repl>"

        override fun readContent(): String = content
    }
}