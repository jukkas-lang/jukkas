package net.ormr.jukkas.lexer

interface Lexer<Token, Type> {
    val isFinished: Boolean
    fun advance(): Token?
    fun createToken(match: StateMatchResult<Type>): Token
}