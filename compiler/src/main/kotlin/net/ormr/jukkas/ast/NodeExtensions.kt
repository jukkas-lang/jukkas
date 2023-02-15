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

package net.ormr.jukkas.ast

import net.ormr.jukkas.Positionable
import net.ormr.jukkas.reporter.MessageType

val Node.closestTable: Table
    get() = ancestors
        .filterIsInstance<TableContainer>()
        .firstOrNull()
        ?.table ?: error("Could not find a table anywhere in the scope for $this")

/**
 * Walks up the hierarchy of `this` [Node] invoking [action] on its [parent][Node.parent] until none is left.
 *
 * Note that the first invocation of [action] will be with `this` as its argument, this differs from [Node.ancestors]
 * which *only* contains the parents of the node.
 *
 * @see [Node.ancestors]
 */
inline fun Node.walkHierarchy(action: (Node) -> Unit) {
    var current: Node? = this
    while (current != null) {
        action(current)
        current = current.parent
    }
}

fun Node.reportSemanticError(position: Positionable, message: String) {
    val unit = compilationUnit
    unit.reporter.reportError(unit.source, MessageType.Error.SEMANTIC, position, message)
}

fun Node.reportTypeError(position: Positionable, message: String) {
    val unit = compilationUnit
    unit.reporter.reportError(unit.source, MessageType.Error.TYPE, position, message)
}

internal fun <T : Node> T.disownParent(): T = apply {
    parent?.disown(this)
}