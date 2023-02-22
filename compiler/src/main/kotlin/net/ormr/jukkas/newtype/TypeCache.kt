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

package net.ormr.jukkas.newtype

class TypeCache {
    private val cache = hashMapOf<String, TypeOrError>()

    fun find(name: String): TypeOrError? = cache[name]

    operator fun contains(name: String): Boolean = name in cache

    fun define(name: String, type: TypeOrError) {
        require(name !in cache) { "Redefining type name: $name" }
        cache[name] = type
    }

    inline fun findOrDefine(name: String, defaultType: () -> TypeOrError): TypeOrError = when (val type = find(name)) {
        null -> {
            val newType = defaultType()
            define(name, newType)
            newType
        }
        else -> type
    }
}