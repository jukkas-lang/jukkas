package net.ormr.jukkas.lexer

interface Lexer<Token, Type> {
    val isFinished: Boolean
    fun advance(): Token?
    fun createToken(match: LexerMatcher.Result<Type>): Token
}

abstract class GenericLexer<Token, Type, Matcher : LexerMatcher<Type, Context>, Context>(
    source: String
) : Lexer<Token, Type> {
    private val scanner = LexerScanner(source)
    abstract val matcher: Matcher
    abstract val context: Context

    final override var isFinished = false
        private set

    override fun advance(): Token? {
        val match = matcher.match(scanner, context)
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
