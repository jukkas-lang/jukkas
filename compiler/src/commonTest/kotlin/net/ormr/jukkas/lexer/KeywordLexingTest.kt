package net.ormr.jukkas.lexer

import io.kotest.core.spec.style.FunSpec
import net.ormr.jukkas.*

class KeywordLexingTest : FunSpec({
    fun testKeywordSpec(spec: List<TokenSpec>) {
        val tokens = parseTokens(spec.joinToString(" ") { it.token })
        tokens shouldMatchTokenSpec spec
    }

    test("Function keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("fun", TokenType.FUN),
                TokenSpec("function", TokenType.IDENTIFIER),
                TokenSpec("fun_ction", TokenType.IDENTIFIER),
                TokenSpec("`fun", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Val keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("val", TokenType.VAL),
                TokenSpec("value", TokenType.IDENTIFIER),
                TokenSpec("val_ue", TokenType.IDENTIFIER),
                TokenSpec("`val", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Var keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("var", TokenType.VAR),
                TokenSpec("variable", TokenType.IDENTIFIER),
                TokenSpec("var_iable", TokenType.IDENTIFIER),
                TokenSpec("`var", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("True keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("true", TokenType.TRUE),
                TokenSpec("truest", TokenType.IDENTIFIER),
                TokenSpec("true_st", TokenType.IDENTIFIER),
                TokenSpec("`true", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("False keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("false", TokenType.FALSE),
                TokenSpec("falsehood", TokenType.IDENTIFIER),
                TokenSpec("false_hood", TokenType.IDENTIFIER),
                TokenSpec("`false", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Return keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("return", TokenType.RETURN),
                TokenSpec("returnable", TokenType.IDENTIFIER),
                TokenSpec("return_able", TokenType.IDENTIFIER),
                TokenSpec("`return", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("If keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("if", TokenType.IF),
                TokenSpec("iffy", TokenType.IDENTIFIER),
                TokenSpec("if_fy", TokenType.IDENTIFIER),
                TokenSpec("`if", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Else keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("else", TokenType.ELSE),
                TokenSpec("elsewhere", TokenType.IDENTIFIER),
                TokenSpec("else_where", TokenType.IDENTIFIER),
                TokenSpec("`else", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("And keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("and", TokenType.AND),
                TokenSpec("android", TokenType.IDENTIFIER),
                TokenSpec("and_roid", TokenType.IDENTIFIER),
                TokenSpec("`and", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Or keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("or", TokenType.OR),
                TokenSpec("orange", TokenType.IDENTIFIER),
                TokenSpec("or_ange", TokenType.IDENTIFIER),
                TokenSpec("`or", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Not keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("not", TokenType.NOT),
                TokenSpec("nothing", TokenType.IDENTIFIER),
                TokenSpec("not_hing", TokenType.IDENTIFIER),
                TokenSpec("`not", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("As keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("as", TokenType.AS),
                TokenSpec("ass", TokenType.IDENTIFIER),
                TokenSpec("as_s", TokenType.IDENTIFIER),
                TokenSpec("`as", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Import keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("import", TokenType.IMPORT),
                TokenSpec("important", TokenType.IDENTIFIER),
                TokenSpec("import_ant", TokenType.IDENTIFIER),
                TokenSpec("`import", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Set keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("set", TokenType.SET),
                TokenSpec("settings", TokenType.IDENTIFIER),
                TokenSpec("set_tings", TokenType.IDENTIFIER),
                TokenSpec("`set", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }

    test("Get keyword") {
        testKeywordSpec(
            listOf(
                TokenSpec("get", TokenType.GET),
                TokenSpec("getter", TokenType.IDENTIFIER),
                TokenSpec("get_ter", TokenType.IDENTIFIER),
                TokenSpec("`get", TokenType.ESCAPED_IDENTIFIER),
            )
        )
    }
})
