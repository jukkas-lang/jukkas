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

import net.ormr.jukkas.ast.Expression
import net.ormr.jukkas.ast.Node
import net.ormr.jukkas.parser.JukkasParser
import org.intellij.lang.annotations.Language

inline fun <T : Node> parseNode(
    @Language("jukkas") source: String,
    crossinline fn: (JukkasParser) -> T,
): JukkasResult<T> = JukkasParser.parse(Source.Text(source), fn)

fun parseExpression(@Language("jukkas") source: String): JukkasResult<Expression> =
    JukkasParser.parse(Source.Text(source), JukkasParser::parseExpression)