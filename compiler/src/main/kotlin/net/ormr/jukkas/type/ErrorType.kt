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

package net.ormr.jukkas.type

class ErrorType(val description: String) : ResolvedType {
    override val jvmName: Nothing
        get() = error("ErrorType: $description")

    override val packageName: Nothing
        get() = error("ErrorType: $description")

    override val simpleName: Nothing
        get() = error("ErrorType: $description")

    override fun toDescriptor(): Nothing = error("ErrorType: $description")
}