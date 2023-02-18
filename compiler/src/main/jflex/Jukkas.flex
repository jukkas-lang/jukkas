package net.ormr.jukkas.lexer;

import net.ormr.jukkas.*;
import net.ormr.jukkas.lexer.TokenType.*;
import java.util.*;

%%

%unicode
%class Lexer
%scanerror JukkasLexerException
%line
%column
%apiprivate
%type TokenType
%eof{
    return;
%eof}
%eofval{
    return null;
%eofval}

%{
    private static final class State {
        final int lBraceCount;
        final int state;

        public State(int state, int lBraceCount) {
            this.state = state;
            this.lBraceCount = lBraceCount;
        }

        @Override
        public String toString() {
            return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
        }
    }

    private final Deque<State> states = new LinkedList<State>();
    private int lBraceCount;

    public Token advance() throws java.io.IOException, JukkasLexerException {
        return token(yylex());
    }

    private void pushState(int state) {
        states.push(new State(yystate(), lBraceCount));
        lBraceCount = 0;
        yybegin(state);
    }

    private void popState() {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        yybegin(state.state);
    }

    private Token token(TokenType type) {
        if (type == null) return null;
        return new Token(type, yytext(), position());
    }

    private Point position() {
        return Point.of(yyline, yycolumn, zzStartRead, zzMarkedPos);
    }

    public void close() throws java.io.IOException {
        yyclose();
    }

    public boolean isAtEnd() {
        return yyatEOF();
    }
%}

Digit = [0-9]
DigitOrUnderscore = [_0-9]
Digits = {Digit} {DigitOrUnderscore}*
HexDigit = [0-9A-Fa-f]
HexDigitOrUnderscore = [_0-9A-Fa-f]
LineTerminator = \r | \n | \r\n
Whitespace = {LineTerminator} | [ \t\f]

Letter = [:letter:] | _
IdentifierPart = [:digit:] | {Letter}
Identifier = {Letter} {IdentifierPart}*
EscapedIdentifier = ` {IdentifierPart}+

IntegerLiteral = {DecimalIntegerLiteral} | {HexIntegerLiteral} | {BinIntegerLiteral}
DecimalIntegerLiteral = (0 | ([1-9]({DigitOrUnderscore})*))
HexIntegerLiteral = 0[Xx]({HexDigitOrUnderscore})*
BinIntegerLiteral = 0[Bb]({DigitOrUnderscore})*

SymbolLiteral = "'" {IdentifierPart}*

CharacterLiteral = "'" ([^\\\'\n] | {EscapeSequence})* "'"
UnicodeEscapeCode = {HexDigit}{HexDigit}{HexDigit}{HexDigit}
UnicodeEscapeName = "{" ([:letter:])* "}"
EscapeSequence = \\(u({UnicodeEscapeCode} | {UnicodeEscapeName}) | [^\n])
StringContent = [^\\\"\n]+
TemplateStart = \\\{

%state STRING
%state STRING_TEMPLATE
%state QUOTE

%%

{Whitespace} { /* do nothing */ }

// literals
// TODO: raw strings, templates
<STRING> {
    {TemplateStart} { pushState(STRING_TEMPLATE); return STRING_TEMPLATE_START.INSTANCE; }
    {EscapeSequence} { return ESCAPE_SEQUENCE.INSTANCE; }
    {StringContent} { return STRING_CONTENT.INSTANCE; }
    \" { popState(); return STRING_END.INSTANCE; }
}
\" { pushState(STRING); return STRING_START.INSTANCE; }

<STRING_TEMPLATE> {
    "{" { lBraceCount++; return LEFT_BRACE.INSTANCE; }
    "}" {
                  if (lBraceCount == 0) {
                      popState();
                      return STRING_TEMPLATE_END.INSTANCE;
                  } else {
                      lBraceCount--;
                      return RIGHT_BRACE.INSTANCE;
                  }
               }
}

{CharacterLiteral} { return CHAR_LITERAL.INSTANCE; }

{IntegerLiteral} { return INT_LITERAL.INSTANCE; }

{SymbolLiteral} { return SYMBOL_LITERAL.INSTANCE; }

// quote
<QUOTE> {
    ``` { popState(); return QUOTE_END.INSTANCE; }
}
``` { pushState(QUOTE); return QUOTE_START.INSTANCE; }

// keywords
"fun" { return FUN.INSTANCE; }
"val" { return VAL.INSTANCE; }
"var" { return VAR.INSTANCE; }
"false" { return FALSE.INSTANCE; }
"true" { return TRUE.INSTANCE; }
"return" { return RETURN.INSTANCE; }
"if" { return IF.INSTANCE; }
"else" { return ELSE.INSTANCE; }
"and" { return AND.INSTANCE; }
"or" { return OR.INSTANCE; }
"not" { return NOT.INSTANCE; }
// TODO: add lexing for this as binary operator with 'as?'
"as" { return AS.INSTANCE; }

// soft keywords
"import" { return IMPORT.INSTANCE; }
"set" { return SET.INSTANCE; }
"get" { return GET.INSTANCE; }

// TODO: handle escaped identifiers
{Identifier} { return IDENTIFIER.INSTANCE; }
{EscapedIdentifier} { return ESCAPED_IDENTIFIER.INSTANCE; }

// operators / separators
"->" { return ARROW.INSTANCE; }
"#{" { return MAP_LITERAL_START.INSTANCE; }
"#(" { return TUPLE_LITERAL_START.INSTANCE; } // TODO: should the longer ones be before or after?
"[" { return LEFT_BRACKET.INSTANCE; }
"]" { return RIGHT_BRACKET.INSTANCE; }
"{" { return LEFT_BRACE.INSTANCE; }
"}" { return RIGHT_BRACE.INSTANCE; }
"(" { return LEFT_PAREN.INSTANCE; }
")" { return RIGHT_PAREN.INSTANCE; }
"|" { return VERTICAL_LINE.INSTANCE; }
"." { return DOT.INSTANCE; }
"+" { return PLUS.INSTANCE; }
"-" { return MINUS.INSTANCE; }
"*" { return STAR.INSTANCE; }
"/" { return SLASH.INSTANCE; }
";" { return SEMICOLON.INSTANCE; }
":" { return COLON.INSTANCE; }
"," { return COMMA.INSTANCE; }
"==" { return EQUAL_EQUAL.INSTANCE; }
"!=" { return BANG_EQUAL.INSTANCE; }
"=" { return EQUAL.INSTANCE; }
"?" { return HOOK.INSTANCE; }
"?." { return HOOK_DOT.INSTANCE; }

// error fallback
[\s\S] { return UNEXPECTED_CHARACTER.INSTANCE; }
<STRING_TEMPLATE, STRING, QUOTE> [\s\S] { return UNEXPECTED_CHARACTER.INSTANCE; }