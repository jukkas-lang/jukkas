package net.ormr.jukkas.lexer

import net.ormr.jukkas.lexer.TokenType.*

typealias JukkasLexerState = FragmentMatchingLexerStateMatcher<TokenType>

@Suppress("VariableNaming")
class JukkasLexer(source: String) : StatefulLexer<Token, TokenType, JukkasLexer.State>(source), FragmentBuilder {
    private val digit = regex("[0-9]")
    private val lineTerminator = regex("""(\r)?\n""")
    private val whitespace = lineTerminator or regex("""[ \t\f]""")

    private val letter = regex("[a-zA-Z_]")
    private val identifierPart = digit or letter
    private val identifier = letter then zeroOrMore(identifierPart)
    private val escapedIdentifier = literal("`") then oneOrMore(identifierPart)

    private val decimalIntLiteral = regex("0|([1-9][0-9_]*)")
    private val hexIntLiteral = regex("0[xX][_0-9A-Fa-f]+")
    private val binIntLiteral = regex("0[bB][_01]+")
    private val intLiteral = decimalIntLiteral or hexIntLiteral or binIntLiteral

    private val escapeSequence = regex("""\\u([0-9-A-Fa-f]{4}|\{[\w_]*\})""")
    private val stringContent = regex("""[^\\"]+""")
    private val templateStart = literal("""\{""")
    private val unexpectedCharacter = regex("""[\s\S]""")

    private val defaultState: JukkasLexerState = State {
        whitespace to { null }
        intLiteral to { INT_LITERAL }

        "fun" to { FUN }
        "val" to { VAL }
        "var" to { VAR }
        "false" to { FALSE }
        "true" to { TRUE }
        "return" to { RETURN }
        "if" to { IF }
        "else" to { ELSE }
        "and" to { AND }
        "or" to { OR }
        "not" to { NOT }
        "as" to { AS }
        "import" to { IMPORT }
        "set" to { SET }
        "get" to { GET }

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
            pushState(stringState)
            STRING_START
        }

        // This should always be last
        unexpectedCharacter to { UNEXPECTED_CHARACTER }
    }

    private val stringState: JukkasLexerState = State {
        templateStart to {
            pushState(stringTemplateState)
            STRING_TEMPLATE_START
        }
        escapeSequence to { ESCAPE_SEQUENCE }
        stringContent to { STRING_CONTENT }
        "\"" to {
            popState()
            STRING_END
        }

        // This should always be last
        unexpectedCharacter to { UNEXPECTED_CHARACTER }
    }

    private val stringTemplateState: JukkasLexerState = State {
        extending(defaultState)
        "{" to {
            templateStringBraceCount++
            LEFT_BRACE
        }
        "}" to {
            when (templateStringBraceCount) {
                0 -> {
                    popState()
                    STRING_TEMPLATE_END
                }
                else -> {
                    templateStringBraceCount--
                    RIGHT_BRACE
                }
            }
        }
    }

    private var templateStringBraceCount = 0

    init {
        pushState(defaultState)
    }

    override fun createToken(match: StateMatchResult<TokenType>): Token =
        Token(match.type, match.fragment.token, match.fragment.span)

    override fun pushState(matcher: LexerStateMatcher<TokenType>) {
        stateStack.addFirst(State(matcher, templateStringBraceCount))
        templateStringBraceCount = 0
    }

    override fun popState(): State {
        val state = stateStack.removeFirst()
        templateStringBraceCount = state.templateStringBraceCount
        return state
    }

    data class State(
        override val matcher: LexerStateMatcher<TokenType>,
        val templateStringBraceCount: Int,
    ) : LexerState<TokenType>
}