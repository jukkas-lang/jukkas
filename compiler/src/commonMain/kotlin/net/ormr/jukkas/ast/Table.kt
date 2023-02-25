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

class Table(val parent: Table? = null) {
    private val entries = hashMapOf<String, NamedDefinition>()

    // TODO: use iteration instead of recursion

    fun find(name: String): NamedDefinition? = entries[name] ?: parent?.find(name)

    fun findLocal(name: String): NamedDefinition? = entries[name]

    fun define(name: String, value: NamedDefinition) {
        if (name in entries) {
            // TODO: rework this, we're not really keeping a reporter on the CompilationUnit anymore
            value.reportSemanticError(value, "Redefined name: '$name'")
        } else {
            entries[name] = value
        }
    }

    fun undefine(name: String) {
        // TODO: verify?
        entries -= name
    }
}