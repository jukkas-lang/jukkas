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

import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source
import net.ormr.jukkas.ast.Node
import net.ormr.jukkas.ast.reportSemanticError
import net.ormr.jukkas.reporter.MessageReporter
import net.ormr.jukkas.reporter.MessageType
import net.ormr.jukkas.type.Type
import kotlin.js.JsName
import kotlin.jvm.JvmName

sealed class CompilerPhase(private val source: Source) {
    protected val reporter: MessageReporter = MessageReporter()

    protected fun reportSemanticError(position: Positionable, message: String) {
        reporter.reportError(source, MessageType.Error.SEMANTIC, position, message)
    }

    @JvmName("receiverReportSemanticError")
    @JsName("receiverReportSemanticError")
    protected fun Node.reportSemanticError(message: String) {
        reportSemanticError(this, message)
    }

    protected fun reportTypeError(position: Positionable, message: String) {
        reporter.reportError(source, MessageType.Error.TYPE, position, message)
    }

    protected fun formatIncompatibleTypes(expected: Type, got: Type): String =
        "Expected type <${expected.asString()}> got <${got.asString()}>"

    protected inline fun <reified T : Any> unreachable(): Nothing =
        error("Branch for <${T::class}> should never be reached")
}
