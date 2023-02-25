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

class ExpressionStatement(expression: Expression) : AbstractStatement() {
    val expression: Expression by child(expression)

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is ExpressionStatement && expression.isStructurallyEquivalent(other.expression)

    override fun toString(): String = expression.toString()

    operator fun component1(): Expression = expression
}