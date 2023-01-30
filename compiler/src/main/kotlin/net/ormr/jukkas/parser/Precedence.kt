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

package net.ormr.jukkas.parser

// https://kotlinlang.org/docs/reference/grammar.html#expressions
internal object Precedence {
    const val ASSIGNMENT = 0
    const val SPREAD = 1
    const val DISJUNCTION = 2 // ||
    const val CONJUNCTION = 3 // &&
    const val EQUALITY = 4
    const val COMPARISON = 5
    const val NAMED_CHECKS = 6 // in, !in, is, !is
    const val NULL_COALESCING = 7
    const val INFIX = 8
    const val RANGE = 9
    const val ADDITIVE = 10
    const val MULTIPLICATIVE = 11
    const val TYPE_RHS = 12 // as, as?
    const val PREFIX = 13
    const val POSTFIX = 14
}