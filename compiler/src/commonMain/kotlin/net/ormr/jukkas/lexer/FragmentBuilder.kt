package net.ormr.jukkas.lexer

interface FragmentBuilder {
    fun literal(segment: String) = LexerFragmentLiteral(segment)
    fun regex(pattern: String) = LexerFragmentRegex(pattern.toRegex())
    fun regex(regex: Regex) = LexerFragmentRegex(regex)
    fun zeroOrMore(fragment: LexerFragment) = LexerFragmentZeroOrMore(fragment)
    fun oneOrMore(fragment: LexerFragment) = LexerFragmentOneOrMore(fragment)
    infix fun LexerFragment.then(second: LexerFragment) = LexerFragmentComposite(this, second)
    infix fun LexerFragment.or(other: LexerFragment) = LexerFragmentAlternative(this, other)
}