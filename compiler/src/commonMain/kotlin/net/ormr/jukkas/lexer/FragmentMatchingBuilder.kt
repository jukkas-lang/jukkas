package net.ormr.jukkas.lexer

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class LexerBuilderDsl

@LexerBuilderDsl
class FragmentMatchingBuilder<Type, Context, Matcher : LexerMatcher<Type, Context>> {
    private val fragments = mutableListOf<FragmentDefinitionCallback<Type, Context>>()
    private val extendedMatchers = mutableListOf<LexerMatcher<Type, Context>>()

    @LexerBuilderDsl
    infix fun String.to(typeCallback: LexerTokenTypeCallback<Type, Context>) {
        val fragment = LexerFragmentLiteral(this)
        val stateFragment = FragmentDefinitionCallback(fragment, typeCallback)
        fragments.add(stateFragment)
    }

    @LexerBuilderDsl
    infix fun LexerFragment.to(typeCallback: LexerTokenTypeCallback<Type, Context>) {
        val stateFragment = FragmentDefinitionCallback(this, typeCallback)
        fragments.add(stateFragment)
    }

    @LexerBuilderDsl
    infix fun extending(matcher: Matcher) {
        extendedMatchers.add(matcher)
    }

    fun build() = LexerFragmentMatcher(fragments, extendedMatchers)
}

@Suppress("FunctionNaming", "FunctionName")
@LexerBuilderDsl
inline fun <Type, Context, Matcher : LexerMatcher<Type, Context>> Matcher(
    builder: FragmentMatchingBuilder<Type, Context, Matcher>.() -> Unit,
): LexerFragmentMatcher<Type, Context> {
    val stateBuilder = FragmentMatchingBuilder<Type, Context, Matcher>()
    builder.invoke(stateBuilder)
    return stateBuilder.build()
}
