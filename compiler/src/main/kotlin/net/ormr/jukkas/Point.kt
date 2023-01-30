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

data class Point(
    val line: Int,
    val column: Int,
    val indices: IntRange,
) : Comparable<Point>, Position {
    companion object {
        @JvmStatic
        fun of(line: Int, column: Int, startIndex: Int, endIndex: Int): Point =
            Point(line, column, startIndex..endIndex)
    }

    init {
        require(line >= 0) { "'line' must be positive" }
        require(column >= 0) { "'column' must be positive" }
    }

    override fun compareTo(other: Point): Int = when {
        line > other.line -> 1
        line < other.line -> -1
        column > other.column -> 1
        column < other.column -> -1
        else -> 0
    }

    //override fun toString(): String = "[${line + 1}:${column + 1}]"
}