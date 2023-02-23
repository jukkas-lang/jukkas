package net.ormr.jukkas.lexer

data class StateMatchResult<Type>(val fragment: FragmentMatchResult, val type: Type)

interface LexerStateMatcher<Type> {
    fun match(scanner: LexerScanner): StateMatchResult<Type>?
}

interface LexerState<Type> {
    val matcher: LexerStateMatcher<Type>
}

abstract class StatefulLexer<Token, Type, State : LexerState<Type>>(source: String) : Lexer<Token, Type> {
    protected val stateStack = ArrayDeque<State>()
    private val scanner = LexerScanner(source)
    final override var isFinished = false
        private set

    private val currentState: State
        get() = stateStack.first()

    abstract fun pushState(matcher: LexerStateMatcher<Type>)

    open fun popState(): State = stateStack.removeFirst()

    override fun advance(): Token? {
        val match = currentState.matcher.match(scanner)
        if (match == null) {
            if (scanner.hasMore) {
                /*
                 * TODO: complain about unexpected token?
                 * Emmit some "garbage" token until something valid is recognized?
                 */
            }
            isFinished = !scanner.hasMore
            return null
        }
        return createToken(match)
    }
}
