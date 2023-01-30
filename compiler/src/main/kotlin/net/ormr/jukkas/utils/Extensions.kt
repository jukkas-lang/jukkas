package net.ormr.jukkas.utils

fun <T> bothNullOrEquivalent(first: T?, second: T?, predicate: (first: T, second: T) -> Boolean): Boolean {
    if (first == null && second == null) {
        return true
    }
    if (first == null || second == null) {
        return false
    }
    return predicate(first, second)
}