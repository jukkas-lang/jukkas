package net.ormr.jukkas.utils

fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}

fun <T> bothNullOrEquivalent(
    first: T?,
    second: T?,
    predicate: (first: T, second: T) -> Boolean,
): Boolean {
    if (first == null && second == null) {
        return true
    }
    if (first == null || second == null) {
        return false
    }
    return predicate(first, second)
}