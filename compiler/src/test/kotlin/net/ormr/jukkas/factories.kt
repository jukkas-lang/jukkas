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

package net.ormr.jukkas

import net.ormr.jukkas.ast.BinaryOperation
import net.ormr.jukkas.ast.BinaryOperator
import net.ormr.jukkas.ast.BooleanLiteral
import net.ormr.jukkas.ast.DefinitionReference
import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.IntLiteral
import net.ormr.jukkas.ast.InvocationArgument
import net.ormr.jukkas.ast.StringLiteral

fun boolean(value: Boolean): BooleanLiteral = BooleanLiteral(value)

fun int(value: Int): IntLiteral = IntLiteral(value)

fun string(value: String): StringLiteral = StringLiteral(value)

fun reference(name: String): DefinitionReference = DefinitionReference(name)

fun binary(left: Expression, operator: BinaryOperator, right: Expression): BinaryOperation =
    BinaryOperation(left, operator, right)

fun argument(value: Expression, name: String? = null): InvocationArgument = InvocationArgument(name, value)