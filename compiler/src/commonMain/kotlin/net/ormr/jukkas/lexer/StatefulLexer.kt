package net.ormr.jukkas.lexer

data class StateMatchResult<Type>(val fragment: FragmentMatchResult, val type: Type)

interface LexerState<Type> {
    fun match(scanner: LexerScanner): StateMatchResult<Type>?
}

abstract class StatefulLexer<Token, Type>(source: String) : Lexer<Token, Type> {
    private val stateStack = ArrayDeque<LexerState<Type>>()
    private val scanner = LexerScanner(source)
    final override var isFinished = false
        private set

    private val currentState: LexerState<Type>
        get() = stateStack.first()

    fun pushState(state: LexerState<Type>) = stateStack.addFirst(state)

    fun popState() = stateStack.removeFirst()

    override fun advance(): Token? {
        val match = currentState.match(scanner)
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
