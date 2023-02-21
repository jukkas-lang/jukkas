package net.ormr.jukkas.lexer

import net.ormr.jukkas.Span

interface LexerFragment {
    fun match(scanner: LexerScanner): FragmentMatchResult?
}

data class FragmentMatchResult(val span: Span, val token: String)

class LexerFragmentLiteral(val literal: String) : LexerFragment {
    override fun match(scanner: LexerScanner): FragmentMatchResult? =
        scanner.tryConsumeSegment(literal.length) { it == literal }
}

class LexerFragmentRegex(val regex: Regex) : LexerFragment {
    override fun match(scanner: LexerScanner): FragmentMatchResult? = scanner.tryConsumeRegex(regex)
}

class LexerFragmentComposite(val first: LexerFragment, val second: LexerFragment) : LexerFragment {
    override fun match(scanner: LexerScanner): FragmentMatchResult? {
        val scannerSnapshot = scanner.snapshot()
        val firstMatch = first.match(scannerSnapshot) ?: return null
        val secondMatch = second.match(scannerSnapshot) ?: return null
        scanner.resumeFromSnapshot(scannerSnapshot)

        val segment = firstMatch.token + secondMatch.token
        val span = Span(firstMatch.span.start, secondMatch.span.end)
        return FragmentMatchResult(span, segment)
    }
}

class LexerFragmentAlternative(val first: LexerFragment, val second: LexerFragment) : LexerFragment {
    override fun match(scanner: LexerScanner): FragmentMatchResult? = first.match(scanner) ?: second.match(scanner)
}

class LexerFragmentZeroOrMore(val fragment: LexerFragment) : LexerFragment {
    override fun match(scanner: LexerScanner): FragmentMatchResult? {
        val matches = buildList {
            while (true) {
                add(fragment.match(scanner) ?: break)
            }
        }

        if (matches.isEmpty()) {
            val point = scanner.getCurrentPoint()
            val span = Span(point, point)
            return FragmentMatchResult(span, "")
        }

        val segment = matches.joinToString("") { it.token }
        val span = Span(matches.first().span.start, matches.first().span.end)
        return FragmentMatchResult(span, segment)
    }
}

class LexerFragmentOneOrMore(val fragment: LexerFragment) : LexerFragment {
    override fun match(scanner: LexerScanner): FragmentMatchResult? {
        val matches = buildList {
            // First one is required
            add(fragment.match(scanner) ?: return null)
            while (true) {
                add(fragment.match(scanner) ?: break)
            }
        }

        if (matches.isEmpty()) {
            val point = scanner.getCurrentPoint()
            val span = Span(point, point)
            return FragmentMatchResult(span, "")
        }

        val segment = matches.joinToString("") { it.token }
        val span = Span(matches.first().span.start, matches.first().span.end)
        return FragmentMatchResult(span, segment)
    }
}

