package net.ormr.jukkas.utils

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