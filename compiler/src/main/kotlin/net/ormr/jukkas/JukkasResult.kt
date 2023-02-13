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

import net.ormr.jukkas.JukkasResult.Failure
import net.ormr.jukkas.JukkasResult.Success
import net.ormr.jukkas.reporter.Message

val Failure.groupedMessages: Map<Source, List<Message>>
    get() = buildMap<Source, MutableList<Message>> {
        for (message in messages) {
            getOrPut(message.source) { mutableListOf() }.add(message)
        }
    }

sealed interface JukkasResult<out T> {
    data class Failure(val messages: List<Message>) : JukkasResult<Nothing>

    data class Success<T>(val value: T, val messages: List<Message>) : JukkasResult<T>
}

inline fun <T, R> JukkasResult<T>.fold(failure: (Failure) -> R, success: (Success<T>) -> R): R = when (this) {
    is Failure -> failure(this)
    is Success -> success(this)
}

inline fun <T> JukkasResult<T>.getOrElse(defaultValue: (Failure) -> T): T = fold(defaultValue) { it.value }