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

package net.ormr.jukkas.utils

import net.ormr.jukkas.lexer.TokenType

// TODO: better name
fun <T> Collection<T>.join(
    separator: String = ", ",
    lastSeparator: String = ", ",
    prefix: String = "",
    postfix: String = "",
    transform: ((T) -> String)? = null,
): String = buildString {
    append(prefix)
    val lastIndex = this@join.size - 1
    for ((i, item) in this@join.withIndex()) {
        append(transform?.invoke(item) ?: item.toString())
        if (i == (lastIndex - 1)) {
            append(lastSeparator)
        } else if (i < (lastIndex - 1)) {
            append(separator)
        }
    }
    append(postfix)
}

internal fun <T> Collection<T>.joinWithOr(): String = join(lastSeparator = " or ")

internal fun <T : TokenType> Set<T>.joinWithOr(): String = sortedBy { it.image }.join(lastSeparator = " or ")