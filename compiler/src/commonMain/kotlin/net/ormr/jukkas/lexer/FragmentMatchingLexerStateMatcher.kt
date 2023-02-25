package net.ormr.jukkas.lexer

typealias LexerTokenTypeCallback<Type, Context> = Context.() -> Type?

data class FragmentDefinitionCallback<Type, Context>(
    val fragment: LexerFragment,
    val typeCallback: LexerTokenTypeCallback<Type, Context>,
)

class LexerFragmentMatcher<Type, Context>(
    private val fragments: List<FragmentDefinitionCallback<Type, Context>>,
    private val extendedStates: List<LexerMatcher<Type, Context>> = emptyList(),
) : LexerMatcher<Type, Context> {
    override fun match(scanner: LexerScanner, context: Context): LexerMatcher.Result<Type>? {
        outer@ while (scanner.hasMore) {
            for (stateFragment in fragments) {
                // Find the first fragment matching at the current offset
                val match = stateFragment.fragment.match(scanner) ?: continue
                // If the token type for the match is null - the match should be ignored
                val type = stateFragment.typeCallback(context) ?: continue@outer
                return LexerMatcher.Result(match, type)
            }
            return extendedStates.firstNotNullOfOrNull {
                it.match(scanner, context)
            }
        }
        return null
    }
}
