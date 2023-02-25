package net.ormr.jukkas.lexer

interface LexerMatcher<Type, Context> {
    fun match(scanner: LexerScanner, context: Context): Result<Type>?

    data class Result<Type>(val fragment: LexerFragment.Result, val type: Type)
}
