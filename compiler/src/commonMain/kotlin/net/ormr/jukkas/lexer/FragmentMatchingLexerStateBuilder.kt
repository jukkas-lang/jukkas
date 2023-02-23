package net.ormr.jukkas.lexer

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class LexerBuilderDsl

@LexerBuilderDsl
class FragmentMatchingLexerStateBuilder<Type> {
    private val fragments = mutableListOf<LexerStateFragment<Type>>()
    private val extendedStates = mutableListOf<LexerStateMatcher<Type>>()

    infix fun String.to(typeCallback: LexerTokenTypeCallback<Type>) {
        val fragment = LexerFragmentLiteral(this)
        val stateFragment = LexerStateFragment(fragment, typeCallback)
        fragments.add(stateFragment)
    }

    infix fun LexerFragment.to(typeCallback: LexerTokenTypeCallback<Type>) {
        val stateFragment = LexerStateFragment(this, typeCallback)
        fragments.add(stateFragment)
    }

    infix fun extending(state: LexerStateMatcher<Type>) {
        extendedStates.add(state)
    }

    fun build() = FragmentMatchingLexerStateMatcher(fragments, extendedStates)
}

@Suppress("FunctionNaming")
inline fun <reified Type> State(
    builder: FragmentMatchingLexerStateBuilder<Type>.() -> Unit
): FragmentMatchingLexerStateMatcher<Type> {
    val stateBuilder = FragmentMatchingLexerStateBuilder<Type>()
    builder.invoke(stateBuilder)
    return stateBuilder.build()
}