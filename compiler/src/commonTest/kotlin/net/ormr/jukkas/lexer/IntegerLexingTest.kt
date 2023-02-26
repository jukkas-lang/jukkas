package net.ormr.jukkas.lexer

import io.kotest.core.spec.style.FunSpec
import net.ormr.jukkas.*

class IntegerLexingTest : FunSpec({
    test("Zero handling") {
        val spec = listOf(
            TokenSpec("0", TokenType.INT_LITERAL),
            TokenSpec("0x1a", TokenType.INT_LITERAL),
            TokenSpec("0b10", TokenType.INT_LITERAL),
        )
        val tokens = parseTokens(spec.joinToString(" ") { it.token })
        tokens shouldMatchTokenSpec spec
    }

    test("Hex handling") {
        val spec = listOf(
            TokenSpec("0x123456789abcdef", TokenType.INT_LITERAL),
            TokenSpec("0x1234_5678_9abc_def", TokenType.INT_LITERAL),
            TokenSpec("0X123456789abcdef", TokenType.INT_LITERAL),
            TokenSpec("0X1234_5678_9abc_def", TokenType.INT_LITERAL),
        )
        val tokens = parseTokens(spec.joinToString(" ") { it.token })
        tokens shouldMatchTokenSpec spec
    }

    test("Binary handling") {
        val spec = listOf(
            TokenSpec("0b00000001", TokenType.INT_LITERAL),
            TokenSpec("0b00_00_00_01", TokenType.INT_LITERAL),
            TokenSpec("0B00000001", TokenType.INT_LITERAL),
            TokenSpec("0B00_00_00_01", TokenType.INT_LITERAL),
        )
        val tokens = parseTokens(spec.joinToString(" ") { it.token })
        tokens shouldMatchTokenSpec spec
    }
})
