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
import net.ormr.jukkas.utils.checkStructuralEquivalence

class ConditionalBranch(
    condition: Expression,
    thenBranch: Expression,
    elseBranch: Expression?,
) : Expression(), HasMutableType {
    var condition: Expression by child(condition)
    var thenBranch: Expression by child(thenBranch)
    var elseBranch: Expression? by child(elseBranch)
    override var type: Type = UnknownType

    override fun isStructurallyEquivalent(other: StructurallyComparable): Boolean =
        other is ConditionalBranch &&
                condition.isStructurallyEquivalent(other.condition) &&
                thenBranch.isStructurallyEquivalent(other.thenBranch) &&
                checkStructuralEquivalence(elseBranch, other.elseBranch) &&
                type.isStructurallyEquivalent(other.type)

    operator fun component1(): Expression = condition

    operator fun component2(): Expression = thenBranch

    operator fun component3(): Expression? = elseBranch

    operator fun component4(): Type = type
}