package net.ormr.jukkas.lexer

typealias JukkasLexerState = FragmentMatchingLexerState<TokenType>

@Suppress("VariableNaming", "PrivatePropertyName")
class JukkasLexer(source: String) : StatefulLexer<Token, TokenType>(source), FragmentBuilder {

    private val DIGIT = regex("[0-9]")
    private val LINE_TERMINATOR = regex("(\\r)?\\n")
    private val WHITESPACE = LINE_TERMINATOR or regex("[ \\t\\f]")

    private val LETTER = regex("[a-zA-Z_]")
    private val IDENTIFIER_PART = DIGIT or LETTER
    private val IDENTIFIER = LETTER then zeroOrMore(IDENTIFIER_PART)
    private val ESCAPED_IDENTIFIER = literal("`") then oneOrMore(IDENTIFIER_PART)

    private val DECIMAL_INTEGER_LITERAL = regex("0|([1-9][0-9_]*)")
    private val HEX_INTEGER_LITERAL = regex("0[xX][_0-9A-Fa-f]+")
    private val BIN_INTEGER_LITERAL = regex("0[bB][_01]+")
    private val INTEGER_LITERAL = DECIMAL_INTEGER_LITERAL or HEX_INTEGER_LITERAL or BIN_INTEGER_LITERAL

    private val ESCAPE_SEQUENCE = regex("""\\u([0-9-A-Fa-f]{4}|\{[\w_]*\})""")
    private val STRING_CONTENT = regex("[^\\\\\"]+")
    private val TEMPLATE_START = literal("\\{")
    private val UNEXPECTED_CHARACTER = regex("[\\s\\S]")

    private val STATE_DEFAULT: JukkasLexerState = State {
        WHITESPACE to { null }
        INTEGER_LITERAL to { TokenType.INT_LITERAL }

        "fun" to { TokenType.FUN }
        "val" to { TokenType.VAL }
        "var" to { TokenType.VAR }
        "false" to { TokenType.FALSE }
        "true" to { TokenType.TRUE }
        "return" to { TokenType.RETURN }
        "if" to { TokenType.IF }
        "else" to { TokenType.ELSE }
        "and" to { TokenType.AND }
        "or" to { TokenType.OR }
        "not" to { TokenType.NOT }
        "as" to { TokenType.AS }
        "import" to { TokenType.IMPORT }
        "set" to { TokenType.SET }
        "get" to { TokenType.GET }

        IDENTIFIER to { TokenType.IDENTIFIER }
        ESCAPED_IDENTIFIER to { TokenType.ESCAPED_IDENTIFIER }

        "->" to { TokenType.ARROW }
        "==" to { TokenType.EQUAL_EQUAL }
        "!=" to { TokenType.BANG_EQUAL }
        "=" to { TokenType.EQUAL }
        "#{" to { TokenType.MAP_LITERAL_START }
        "#(" to { TokenType.TUPLE_LITERAL_START }
        "?." to { TokenType.HOOK_DOT }
        "?" to { TokenType.HOOK }
        "[" to { TokenType.LEFT_BRACKET }
        "]" to { TokenType.RIGHT_BRACKET }
        "{" to { TokenType.LEFT_BRACE }
        "}" to { TokenType.RIGHT_BRACE }
        "(" to { TokenType.LEFT_PAREN }
        ")" to { TokenType.RIGHT_PAREN }
        "|" to { TokenType.VERTICAL_LINE }
        "." to { TokenType.DOT }
        "+" to { TokenType.PLUS }
        "-" to { TokenType.MINUS }
        "*" to { TokenType.STAR }
        "/" to { TokenType.SLASH }
        ";" to { TokenType.SEMICOLON }
        ":" to { TokenType.COLON }
        "," to { TokenType.COMMA }
        "\"" to { pushState(STATE_STRING); TokenType.STRING_START }

        // This should always be last
        UNEXPECTED_CHARACTER to { TokenType.UNEXPECTED_CHARACTER }
    }

    private val STATE_STRING: JukkasLexerState = State {
        TEMPLATE_START to { pushState(STATE_STRING_TEMPLATE); TokenType.STRING_TEMPLATE_START }
        ESCAPE_SEQUENCE to { TokenType.ESCAPE_SEQUENCE }
        STRING_CONTENT to { TokenType.STRING_CONTENT }
        "\"" to { popState(); TokenType.STRING_END }

        // This should always be last
        UNEXPECTED_CHARACTER to { TokenType.UNEXPECTED_CHARACTER }
    }

    private val STATE_STRING_TEMPLATE: JukkasLexerState = State {
        extending(STATE_DEFAULT)
        "{" to { templateStringBraceCount++; TokenType.LEFT_BRACE }
        "}" to {
            when (templateStringBraceCount) {
                0 -> {
                    popState()
                    TokenType.STRING_TEMPLATE_END
                }
                else -> {
                    templateStringBraceCount--
                    TokenType.RIGHT_BRACE
                }
            }
        }
    }

    private var templateStringBraceCount = 0

    init {
        pushState(STATE_DEFAULT)
    }

    override fun createToken(match: StateMatchResult<TokenType>): Token {
        return Token(match.type, match.fragment.token, match.fragment.span)
    }
}