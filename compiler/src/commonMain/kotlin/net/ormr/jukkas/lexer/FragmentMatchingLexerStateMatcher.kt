package net.ormr.jukkas.lexer

typealias LexerTokenTypeCallback<Type> = () -> Type?

data class LexerStateFragment<Type>(val fragment: LexerFragment, val typeCallback: LexerTokenTypeCallback<Type>)

class FragmentMatchingLexerStateMatcher<Type>(
    private val fragments: List<LexerStateFragment<Type>>,
    private val extendedStates: List<LexerStateMatcher<Type>> = emptyList(),
) : LexerStateMatcher<Type> {
    override fun match(scanner: LexerScanner): StateMatchResult<Type>? {
        outer@ while (scanner.hasMore) {
            for (stateFragment in fragments) {
                // Find the first fragment matching at the current offset
                val match = stateFragment.fragment.match(scanner) ?: continue
                // If the token type for the match is null - the match should be ignored
                val type = stateFragment.typeCallback() ?: continue@outer
                return StateMatchResult(match, type)
            }
            return extendedStates.firstNotNullOfOrNull {
                it.match(scanner)
            }
        }
        return null
    }
}