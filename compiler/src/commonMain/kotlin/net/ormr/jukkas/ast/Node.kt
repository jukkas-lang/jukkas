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

import net.ormr.jukkas.Position
import net.ormr.jukkas.Positionable
import net.ormr.jukkas.StructurallyComparable

sealed interface Node : Positionable, StructurallyComparable {
    var parent: Node?
    val position: Position?
    val closestPosition: Position?
        get() = position ?: parent?.closestPosition

    /**
     * Returns a sequence of all the ancestors of `this` [Node].
     *
     * Note that the returned sequence does *not* contain `this` node.
     *
     * @see [walkHierarchy]
     */
    val ancestors: Sequence<Node>
        get() = generateSequence(parent) { it.parent }

    val compilationUnit: CompilationUnit

    override fun findPosition(): Position = closestPosition ?: error("Could not find any position for $this")

    override fun findPositionOrNull(): Position? = position

    fun <T : Node> adopt(child: T): T

    fun <T : Node> disown(child: T): T
}