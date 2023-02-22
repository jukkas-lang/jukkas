package net.ormr.jukkas.ast

import net.ormr.jukkas.StructurallyComparable
import net.ormr.jukkas.type.TypeOrError
import net.ormr.jukkas.type.JvmReferenceType
import net.ormr.jukkas.utils.checkStructuralEquivalence

sealed class StringTemplatePart : ChildNode() {
    class LiteralPart(val literal: StringLiteral) : StringTemplatePart() {
        override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
            other is LiteralPart && literal.isStructurallyEquivalent(other.literal)
    }

    class ExpressionPart(val expression: Expression) : StringTemplatePart() {
        override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
            other is ExpressionPart && expression.isStructurallyEquivalent(other.expression)
    }
}

class StringTemplateExpression(parts: List<StringTemplatePart>) : Expression() {
    val parts: MutableNodeList<StringTemplatePart> = parts.toMutableNodeList(this)

    override val resolvedType: TypeOrError?
        get() = JvmReferenceType.STRING

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is StringTemplateExpression &&
            checkStructuralEquivalence(parts, other.parts) &&
            resolvedType.isStructurallyEquivalent(other.resolvedType)
}