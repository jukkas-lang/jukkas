package net.ormr.jukkas.utils

import net.ormr.jukkas.ast.Node

private val unicodePattern = "\\\\u([0-9A-Fa-f]{4})".toRegex()

// TODO: do we want to make a manual escaper for this instead of using a regex?
fun String.unescapeUnicode(): String = replace(unicodePattern) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}

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