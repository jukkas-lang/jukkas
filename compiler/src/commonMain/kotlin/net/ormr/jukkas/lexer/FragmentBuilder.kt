package net.ormr.jukkas.lexer

interface FragmentBuilder {
    @LexerBuilderDsl
    fun literal(segment: String) = LexerFragmentLiteral(segment)

    @LexerBuilderDsl
    fun regex(pattern: String) = LexerFragmentRegex(pattern.toRegex())

    @LexerBuilderDsl
    fun regex(regex: Regex) = LexerFragmentRegex(regex)

    @LexerBuilderDsl
    fun keyword(keyword: String) = LexerFragmentKeyword(keyword)

    @LexerBuilderDsl
    fun zeroOrMore(fragment: LexerFragment) = LexerFragmentZeroOrMore(fragment)

    @LexerBuilderDsl
    fun oneOrMore(fragment: LexerFragment) = LexerFragmentOneOrMore(fragment)

    @LexerBuilderDsl
    fun optional(fragment: LexerFragment) = LexerFragmentOptional(fragment)

    @LexerBuilderDsl
    infix fun LexerFragment.then(second: LexerFragment) = LexerFragmentComposite(this, second)

    @LexerBuilderDsl
    operator fun LexerFragment.plus(second: LexerFragment) = then(second)

    @LexerBuilderDsl
    infix fun LexerFragment.or(other: LexerFragment) = LexerFragmentAlternative(this, other)
}