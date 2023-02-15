package net.ormr.jukkas.ast

import net.ormr.jukkas.type.JvmReferenceType
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.utils.checkStructuralEquivalence

sealed class StringTemplatePart : ChildNode() {
    class LiteralPart(val literal: StringLiteral) : StringTemplatePart() {
        override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitLiteral(literal)

        override fun isStructurallyEquivalent(other: Node): Boolean =
            other is LiteralPart && literal.isStructurallyEquivalent(other.literal)
    }

    class ExpressionPart(val expression: Expression) : StringTemplatePart() {
        override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitExpression(expression)

        override fun isStructurallyEquivalent(other: Node): Boolean =
            other is ExpressionPart && expression.isStructurallyEquivalent(other.expression)
    }
}

class StringTemplateExpression(parts: List<StringTemplatePart>) : Expression() {
    val parts: MutableNodeList<StringTemplatePart> = parts.toMutableNodeList(this)

    override val type: Type
        get() = JvmReferenceType.STRING

    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitStringTemplateExpression(this)

    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is StringTemplateExpression && checkStructuralEquivalence(parts, other.parts)
}