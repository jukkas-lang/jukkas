package net.ormr.jukkas.lexer

import net.ormr.jukkas.Point
import net.ormr.jukkas.Span

class LexerScanner(val source: String) {
    private var offset = 0
    private var line = 1
    private var column = 0

    val hasMore: Boolean
        get() = offset < source.length

    fun getCurrentPoint() = Point(line, column)

    private fun advance() {
        require(hasMore) { "Lexer is trying to advance past the end of input" }
        offset++
        if (source[offset - 1] == '\n') {
            line++
            column = 0
        } else {
            column++
        }
    }

    fun advance(amount: Int) = repeat(amount) { advance() }

    private fun consume(string: String): FragmentMatchResult {
        val start = getCurrentPoint()
        advance(string.length)
        val end = getCurrentPoint()
        return FragmentMatchResult(Span(start, end), string)
    }

    fun tryConsumeSegment(amount: Int, predicate: (String) -> Boolean): FragmentMatchResult? {
        if (offset + amount > source.length) {
            return null
        }
        val segment = source.substring(offset, offset + amount)
        if (!predicate(segment)) {
            return null
        }
        return consume(segment)
    }

    fun tryConsumeRegex(regex: Regex): FragmentMatchResult? {
        val result = regex.matchAt(source, offset) ?: return null
        return consume(result.value)
    }

    fun snapshot(): LexerScanner = LexerScanner(source).also { cloned ->
        cloned.offset = offset
        cloned.line = line
        cloned.column = column
    }

    fun resumeFromSnapshot(other: LexerScanner) {
        offset = other.offset
        line = other.line
        column = other.column
    }
}