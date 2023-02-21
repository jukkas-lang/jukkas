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

data class Span(val start: Point, val end: Point) : Comparable<Span>, Position {
    init {
        require(start <= end) { "'start' must not occur before 'end'. ($start <= $end)" }
    }

    override fun compareTo(other: Span): Int = when {
        start > other.start -> 1
        start < other.start -> -1
        end > other.end -> 1
        end < other.end -> -1
        else -> 0
    }

    override fun toString(): String = "[$start -> $end]"
}

fun createSpan(from: Positionable, to: Positionable): Span =
    Span(from.findPosition().startPoint, to.findPosition().endPoint)