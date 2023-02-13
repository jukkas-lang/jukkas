/*
 * Copyright 2023 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ormr.jukkas.parser.parselets.prefix

import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.StringLiteral
import net.ormr.jukkas.ast.StringTemplateExpression
import net.ormr.jukkas.ast.StringTemplatePart
import net.ormr.jukkas.ast.withPosition
import net.ormr.jukkas.createSpan
import net.ormr.jukkas.lexer.Token
import net.ormr.jukkas.lexer.TokenType
import net.ormr.jukkas.lexer.TokenType.ESCAPE_SEQUENCE
import net.ormr.jukkas.lexer.TokenType.STRING_CONTENT
import net.ormr.jukkas.lexer.TokenType.STRING_TEMPLATE_END
import net.ormr.jukkas.lexer.TokenType.STRING_TEMPLATE_START
import net.ormr.jukkas.parser.JukkasParser
import net.ormr.jukkas.utils.unescapeUnicode

object StringParselet : PrefixParselet {
    override fun parse(parser: JukkasParser, token: Token): Expression = parser with {
        val parts = buildList {
            while (!check(TokenType.STRING_END) && hasMore()) {
                add(parseLiteralOrTemplate(parser))
            }
        }
        val end = consume(TokenType.STRING_END)

        when {
            // If the string does not have any template variables, just join all parts into a single literal
            // TODO: may be a good idea to also merge consecutive literals
            parts.all { it is StringTemplatePart.LiteralPart } -> {
                val literalParts = parts as List<StringTemplatePart.LiteralPart>
                StringLiteral(literalParts.joinToString("") { it.literal.value }) withPosition createSpan(token, end)
            }
            else -> StringTemplateExpression(parts) withPosition createSpan(token, end)
        }
    }

    private fun parseLiteralOrTemplate(parser: JukkasParser): StringTemplatePart = parser with {
        when {
            match(STRING_CONTENT) -> {
                val content = previous()
                val text = content.text
                val literal = StringLiteral(text) withPosition content
                StringTemplatePart.LiteralPart(literal) withPosition content
            }
            match(ESCAPE_SEQUENCE) -> {
                val sequence = previous()
                val text = previous().text.unescapeUnicode()
                val literal = StringLiteral(text) withPosition sequence
                StringTemplatePart.LiteralPart(literal) withPosition sequence
            }
            match(STRING_TEMPLATE_START) -> {
                val start = current()
                val expression = parseExpression()
                val end = consume(STRING_TEMPLATE_END)
                StringTemplatePart.ExpressionPart(expression withPosition createSpan(start, end))
            }
            else -> error("Unexpected token in string: <${consume()}>")
        }
    }
}