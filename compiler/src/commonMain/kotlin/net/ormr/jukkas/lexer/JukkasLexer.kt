package net.ormr.jukkas.lexer

import net.ormr.jukkas.lexer.TokenType.*

typealias JukkasMatcher = LexerMatcher<TokenType, JukkasLexerContext>

class JukkasLexerContext {
    private val stateStack = ArrayDeque(listOf(JukkasLexerRules.defaultMatcher))
    var templateStringBraceCount = 0

    val currentMatcher: JukkasMatcher
        get() = stateStack.first()

    fun pushMatcher(state: JukkasMatcher) = stateStack.addFirst(state)

    fun popMatcher() = stateStack.removeFirst()
}

object JukkasLexerRules : FragmentBuilder {
    private val digit = regex("[0-9]")
    private val digitOrUnderscore = regex("[_0-9]")
    private val digits = regex("[0-9][_0-9]*")
    private val lineTerminator = regex("""(\r)?\n""")
    private val whitespace = lineTerminator or regex("""[ \t\f]""")

    private val letter = regex("[a-zA-Z_]")
    private val identifierPart = digit or letter
    private val identifier = letter + zeroOrMore(identifierPart)
    private val escapedIdentifier = literal("`") + oneOrMore(identifierPart)

    private val zero = literal("0")
    private val decimalIntLiteral = regex("([1-9][0-9_]*)")
    private val hexIntLiteral = regex("0[xX][_0-9A-Fa-f]+")
    private val binIntLiteral = regex("0[bB][_01]+")
    private val intLiteral = zero or decimalIntLiteral or hexIntLiteral or binIntLiteral + optional(regex("[lL]"))

    private val dot = literal(".")
    private val floatPostfix = regex("[Ff]")
    private val exponentPart = regex("""[Ee][+\-]?""") + zeroOrMore(digitOrUnderscore)
    private val floatingPointLiteral1 =
        digits + dot + oneOrMore(digit) + optional(exponentPart) + optional(floatPostfix)
    private val floatingPointLiteral2 = dot + digits + optional(exponentPart) + optional(floatPostfix)
    private val floatingPointLiteral3 = digits + exponentPart + optional(floatPostfix)
    private val floatingPointLiteral4 = digits + floatPostfix
    private val doubleLiteral =
        floatingPointLiteral1 or floatingPointLiteral2 or floatingPointLiteral3 or floatingPointLiteral4

    private val escapeSequence = regex("""\\u([0-9-A-Fa-f]{4}|\{[\w_]*\})""")
    private val stringContent = regex("""[^\\"]+""")
    private val templateStart = literal("""\{""")
    private val unexpectedCharacter = regex("""[\s\S]""")

    val defaultMatcher: JukkasMatcher = Matcher {
        whitespace to { null }
        doubleLiteral to { DOUBLE_LITERAL }
        intLiteral to { INT_LITERAL }

        keyword("fun") to { FUN }
        keyword("val") to { VAL }
        keyword("var") to { VAR }
        keyword("false") to { FALSE }
        keyword("true") to { TRUE }
        keyword("return") to { RETURN }
        keyword("if") to { IF }
        keyword("else") to { ELSE }
        keyword("and") to { AND }
        keyword("or") to { OR }
        keyword("not") to { NOT }
        keyword("as") to { AS }
        keyword("import") to { IMPORT }
        keyword("set") to { SET }
        keyword("get") to { GET }

        identifier to { IDENTIFIER }
        escapedIdentifier to { ESCAPED_IDENTIFIER }

        "->" to { ARROW }
        "==" to { EQUAL_EQUAL }
        "!=" to { BANG_EQUAL }
        "=" to { EQUAL }
        "#{" to { MAP_LITERAL_START }
        "#(" to { TUPLE_LITERAL_START }
        "?." to { HOOK_DOT }
        "?" to { HOOK }
        "[" to { LEFT_BRACKET }
        "]" to { RIGHT_BRACKET }
        "{" to { LEFT_BRACE }
        "}" to { RIGHT_BRACE }
        "(" to { LEFT_PAREN }
        ")" to { RIGHT_PAREN }
        "|" to { VERTICAL_LINE }
        "." to { DOT }
        "+" to { PLUS }
        "-" to { MINUS }
        "*" to { STAR }
        "/" to { SLASH }
        ";" to { SEMICOLON }
        ":" to { COLON }
        "," to { COMMA }
        "\"" to {
            pushMatcher(stringMatcher)
            STRING_START
        }

        // This should always be last
        unexpectedCharacter to { UNEXPECTED_CHARACTER }
    }

    val stringMatcher: JukkasMatcher = Matcher {
        templateStart to {
            pushMatcher(stringTemplateMatcher)
            STRING_TEMPLATE_START
        }
        escapeSequence to { ESCAPE_SEQUENCE }
        stringContent to { STRING_CONTENT }
        "\"" to {
            popMatcher()
            STRING_END
        }

        // This should always be last
        unexpectedCharacter to { UNEXPECTED_CHARACTER }
    }

    val stringTemplateMatcher: JukkasMatcher = Matcher {
        extending(defaultMatcher)
        "{" to {
            templateStringBraceCount++
            LEFT_BRACE
        }
        "}" to {
            when (templateStringBraceCount) {
                0 -> {
                    popMatcher()
                    STRING_TEMPLATE_END
                }
                else -> {
                    templateStringBraceCount--
                    RIGHT_BRACE
                }
            }
        }
    }
}

@Suppress("VariableNaming")
class JukkasLexer(source: String) : GenericLexer<Token, TokenType, JukkasMatcher, JukkasLexerContext>(source) {
    override val context: JukkasLexerContext = JukkasLexerContext()

    override val matcher: JukkasMatcher
        get() = context.currentMatcher

    override fun createToken(match: LexerMatcher.Result<TokenType>): Token =
        Token(match.type, match.fragment.token, match.fragment.span)
}
