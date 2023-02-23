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

package net.ormr.jukkas.reporter

import net.ormr.jukkas.JukkasResult
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.Source

class MessageReporter {
    private val _messages = mutableListOf<Message>()

    val messages: List<Message>
        get() = _messages

    val errors: List<Message.Error>
        get() = _messages.filterIsInstance<Message.Error>()

    inline fun <T> toResult(action: () -> T): JukkasResult<T> {
        val value = action()
        return if (hasErrors()) JukkasResult.Failure(messages) else JukkasResult.Success(value, messages)
    }

    fun hasErrors(): Boolean = _messages.any { it is Message.Error }

    fun reportWarning(
        source: Source,
        type: MessageType.Warning,
        position: Positionable,
        message: String,
    ): Message.Warning {
        val warning = Message.Warning(source, type, position.findPosition(), message)
        _messages += warning
        return warning
    }

    fun reportError(
        source: Source,
        type: MessageType.Error,
        position: Positionable,
        message: String,
    ): Message.Error {
        val error = Message.Error(source, type, position.findPosition(), message)
        _messages += error
        return error
    }
}