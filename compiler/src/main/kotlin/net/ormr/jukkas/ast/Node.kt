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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class Node : Positionable {
    open var parent: Node? = null

    abstract val position: Position?

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

    abstract val compilationUnit: CompilationUnit

    /**
     * Returns `true` if [other] is structurally equivalent to `this` node.
     *
     * Two nodes being structurally equivalent is *not* the same as the two nodes being [equal][Node.equals]. This is
     * because structural equivalence checks leaves out certain properties when comparing two instances, one of which
     * is the `type` of a node.
     *
     * Structural equivalence checks are intended for use via unit tests, and should probably not be used outside of
     * unit tests.
     */
    abstract fun isStructurallyEquivalent(other: Node): Boolean

    override fun findPosition(): Position = closestPosition ?: error("Could not find any position for $this")

    override fun findPositionOrNull(): Position? = position

    abstract fun <T> accept(visitor: NodeVisitor<T>): T

    fun <T : Node> adopt(child: T): T {
        if (child.parent !== this) {
            child.disownParent()
            child.parent = this
            childAdopted(child)
        }

        return child
    }

    protected open fun childAdopted(child: Node) {}

    fun <T : Node> disown(child: T): T {
        if (child.parent === this) {
            child.parent = null
            childDisowned(child)
        }

        return child
    }

    protected open fun childDisowned(child: Node) {}

    protected fun <T : Node> child(
        initial: T,
        setterCallback: ((T) -> Unit)? = null,
    ): ReadWriteProperty<Node, T> = ChildProperty(this, initial, setterCallback)

    @JvmName("nullableChild")
    protected fun <T : Node> child(
        initial: T?,
        setterCallback: ((T?) -> Unit)? = null,
    ): ReadWriteProperty<Node, T?> = NullableChildProperty(this, initial, setterCallback)

    private class ChildProperty<T : Node>(
        parent: Node,
        private var child: T,
        private val setterCallback: ((T) -> Unit)?,
    ) : ReadWriteProperty<Node, T> {
        init {
            val child = child
            setterCallback?.invoke(child)
            parent.adopt(child)
        }

        override fun getValue(thisRef: Node, property: KProperty<*>): T = child

        override fun setValue(
            thisRef: Node,
            property: KProperty<*>,
            value: T,
        ) {
            setterCallback?.invoke(value)
            child = thisRef.adopt(value)
        }
    }

    private class NullableChildProperty<T : Node>(
        parent: Node,
        private var child: T?,
        private val setterCallback: ((T?) -> Unit)?,
    ) : ReadWriteProperty<Node, T?> {
        init {
            val child = child
            setterCallback?.invoke(child)
            if (child != null) parent.adopt(child)
        }

        override fun getValue(thisRef: Node, property: KProperty<*>): T? = child

        override fun setValue(
            thisRef: Node,
            property: KProperty<*>,
            value: T?,
        ) {
            setterCallback?.invoke(value)
            child = value?.let(thisRef::adopt)
        }
    }
}