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

sealed interface NodeList<out T : Node> : List<T>

sealed interface MutableNodeList<T : Node> : NodeList<T>, MutableList<T> {
    fun observe(
        onAdd: ((index: Int, node: T) -> Unit)? = null,
        onRemove: ((index: Int, node: T) -> Unit)? = null,
    ): MutableNodeList<T>
}

private object EmptyNodeList : NodeList<Nothing>, List<Nothing> by emptyList()

private class MutableNodeListImpl<T : Node>(
    private val parent: Node,
    initialCapacity: Int = -1,
) : MutableNodeList<T>, AbstractMutableList<T>() {
    private val addObservers = mutableListOf<(index: Int, node: T) -> Unit>()
    private val removeObservers = mutableListOf<(index: Int, node: T) -> Unit>()
    private val delegate: MutableList<T> = if (initialCapacity != -1) ArrayList(initialCapacity) else ArrayList()

    override val size: Int
        get() = delegate.size

    override fun observe(
        onAdd: ((index: Int, node: T) -> Unit)?,
        onRemove: ((index: Int, node: T) -> Unit)?,
    ): MutableNodeList<T> = apply {
        if (onAdd != null) addObservers += onAdd
        if (onRemove != null) removeObservers += onRemove
    }

    override fun add(index: Int, element: T) {
        parent.adopt(element)
        delegate.add(index, element)
        addObservers.forEach { it(index, element) }
    }

    override fun get(index: Int): T = delegate[index]

    override fun removeAt(index: Int): T {
        val previous = delegate.removeAt(index)
        previous.disownParent()
        removeObservers.forEach { it(index, previous) }
        return previous
    }

    override fun set(index: Int, element: T): T {
        parent.adopt(element)
        val previous = delegate.set(index, element)
        addObservers.forEach { it(index, element) }
        removeObservers.forEach { it(index, previous) }
        return previous
    }
}

fun <T : Node> Iterable<T>.toNodeList(parent: Node): NodeList<T> = when (this) {
    is Collection -> toNodeList(parent)
    else -> MutableNodeListImpl<T>(parent).also { it.addAll(this) }
}

fun <T : Node> Collection<T>.toNodeList(parent: Node): NodeList<T> =
    if (isEmpty()) EmptyNodeList else MutableNodeListImpl<T>(parent).also { it.addAll(this) }

fun <T : Node> Iterable<T>.toMutableNodeList(
    parent: Node,
    onAdd: ((index: Int, node: T) -> Unit)? = null,
    onRemove: ((index: Int, node: T) -> Unit)? = null,
): MutableNodeList<T> {
    val list = when (this) {
        is Collection -> MutableNodeListImpl<T>(parent, size)
        else -> MutableNodeListImpl(parent)
    }.observe(onAdd, onRemove)
    list.addAll(this)
    return list
}

fun <T : Node> emptyNodeList(): NodeList<T> = EmptyNodeList