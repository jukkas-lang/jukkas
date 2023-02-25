package net.ormr.jukkas.lexer

interface FragmentBuilder {
    @LexerBuilderDsl
    fun literal(segment: String) = LexerFragmentLiteral(segment)

    @LexerBuilderDsl
    fun regex(pattern: String) = LexerFragmentRegex(pattern.toRegex())

    @LexerBuilderDsl
    fun regex(regex: Regex) = LexerFragmentRegex(regex)

    @LexerBuilderDsl
    fun zeroOrMore(fragment: LexerFragment) = LexerFragmentZeroOrMore(fragment)

    @LexerBuilderDsl
    fun oneOrMore(fragment: LexerFragment) = LexerFragmentOneOrMore(fragment)

    @LexerBuilderDsl
    fun optional(fragment: LexerFragment) = LexerFragmentOptional(fragment)

    @LexerBuilderDsl
    infix fun LexerFragment.then(second: LexerFragment) = LexerFragmentComposite(this, second)

    @LexerBuilderDsl
    infix fun LexerFragment.or(other: LexerFragment) = LexerFragmentAlternative(this, other)
}