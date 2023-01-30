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

import net.ormr.jukkas.Position
import net.ormr.jukkas.Source

sealed interface Message {
    val source: Source
    val type: MessageType
    val position: Position
    val message: String

    data class Warning(
        override val source: Source,
        override val type: MessageType.Warning,
        override val position: Position,
        override val message: String,
    ) : Message

    data class Error(
        override val source: Source,
        override val type: MessageType.Error,
        override val position: Position,
        override val message: String,
    ) : Message
}