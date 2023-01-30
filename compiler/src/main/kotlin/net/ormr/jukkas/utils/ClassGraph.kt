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

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.ScanResult

internal fun scanForClass(name: String): ClassInfo? = scanClassGraph { it.getClassInfo(name) }

internal inline fun <R> scanClassGraph(
    builder: ClassGraph.() -> Unit = { enableAllInfo() },
    block: (ScanResult) -> R,
): R {
    val graph = ClassGraph().apply(builder)
    return graph.scan().use(block)
}