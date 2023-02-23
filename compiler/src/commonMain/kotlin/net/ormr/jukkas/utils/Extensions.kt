package net.ormr.jukkas.utils

import net.ormr.jukkas.ast.Node

inline fun <T> bothNullOrEquivalent(
    first: T?,
    second: T?,
    predicate: (first: T, second: T) -> Boolean,
): Boolean = when {
    first == null && second == null -> true
    first == null || second == null -> false
    else -> predicate(first, second)
}

fun checkStructuralEquivalence(first: Node?, second: Node?): Boolean =
    bothNullOrEquivalent(first, second) { a, b -> a.isStructurallyEquivalent(b) }

/**
 * Returns `true` if all elements of [a] and [b] are [structurally equivalent][Node.isStructurallyEquivalent],
 * otherwise `false`.
 *
 * If `a` and `b` do not have the same size, then `false` will be returned before any structural equivalence checks
 * are made.
 */
fun checkStructuralEquivalence(
    a: Collection<Node>,
    b: Collection<Node>,
): Boolean = a.size == b.size && (a.asSequence() zip b.asSequence()).all { (a, b) -> a.isStructurallyEquivalent(b) }