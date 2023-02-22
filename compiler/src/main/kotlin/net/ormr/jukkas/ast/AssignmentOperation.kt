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

import net.ormr.jukkas.StructurallyComparable
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.UnknownType

// assignments are *not* expressions
class AssignmentOperation(
    left: Expression,
    val operator: AssignmentOperator,
    value: Expression,
) : Expression(), HasMutableType {
    var left: Expression by child(left)
    var value: Expression by child(value)
    override var type: Type = UnknownType

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is AssignmentOperation &&
            operator == other.operator &&
            left.isStructurallyEquivalent(other.left) &&
            value.isStructurallyEquivalent(other.value) &&
            type.isStructurallyEquivalent(other.type)

    override fun toString(): String = "(= $left $value)"

    operator fun component1(): Expression = left

    operator fun component2(): AssignmentOperator = operator

    operator fun component3(): Expression = value

    operator fun component4(): Type = type
}

enum class AssignmentOperator(override val symbol: String) : Operator {
    BASIC("=");

    companion object {
        private val symbolToInstance = values().associateByTo(hashMapOf()) { it.symbol }

        fun fromSymbolOrNull(symbol: String): AssignmentOperator? = symbolToInstance[symbol]
    }
}