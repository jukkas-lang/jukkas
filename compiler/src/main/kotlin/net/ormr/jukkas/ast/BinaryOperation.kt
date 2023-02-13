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

package net.ormr.jukkas.ast

import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.UnknownType

class BinaryOperation(
left: Expression,
 val operator: BinaryOperator,
 right: Expression,
) : Expression() {
    var left: Expression by child(left)
    var right: Expression by child(right)
    override var type: Type = UnknownType

    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitBinaryOperation(this)

    override fun isStructurallyEquivalent(other: Node): Boolean =
        other is BinaryOperation && operator == other.operator && left.isStructurallyEquivalent(other.left) &&
 right.isStructurallyEquivalent(other.right)

    override fun toString(): String = "(${operator.symbol} $left $right)"
}

enum class BinaryOperator(override val symbol: String) : Operator {
    NOT("not"),

    PLUS("+"),
    MINUS("-"),

    MULTIPLICATION("*"),
    DIVISION("/"),

    EQUALS("=="),
    NOT_EQUALS("!=="),

    AND("and"),
    OR("or");

    companion object {
        private val symbolToInstance = values().associateByTo(hashMapOf()) { it.symbol }

        fun fromSymbolOrNull(symbol: String): BinaryOperator? = symbolToInstance[symbol]
    }
}