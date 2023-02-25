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

package net.ormr.jukkas.type.member

import net.ormr.jukkas.type.Type

internal object JvmFunctionComparator : Comparator<TypeMember.Function> {
    override fun compare(o1: TypeMember.Function?, o2: TypeMember.Function?): Int {
        // TODO: support non JVM methods
        require(o1 is JvmMethod) { "Only JVM methods allowed for now" }
        require(o2 is JvmMethod) { "Only JVM methods allowed for now" }
        return when {
            o1.member.isVarArgs && !(o2.member.isVarArgs) -> 1
            // TODO: should it be 'o1' or 'o2' here
            o2.member.isVarArgs && !(o1.member.isVarArgs) -> -1
            else -> compareTypes(o1.parameterTypes, o2.parameterTypes)
        }
    }

    private fun compareTypes(a: List<Type>, b: List<Type>): Int {
        when {
            a.size != b.size -> return a.size.compareTo(b.size)
            else -> {
                for (i in a.indices) {
                    val cmp = a[i] compareCompatibilityTo b[i]

                    if (cmp != 0) {
                        return cmp
                    }
                }

                return 0
            }
        }
    }
}