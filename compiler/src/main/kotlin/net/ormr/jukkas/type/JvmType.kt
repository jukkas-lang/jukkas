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

import io.github.classgraph.ClassInfo
import net.ormr.jukkas.utils.scanForClass

class JvmType(val classInfo: ClassInfo) : ResolvedType {
    override val internalName: String = "${packageName.replace('.', '/')}/${simpleName.replace('$', '.')}"

    // empty if located in root package
    override val packageName: String
        get() = classInfo.packageName

    override val simpleName: String
        get() = classInfo.simpleName

    override fun toJvmDescriptor(): String = "L$internalName;"

    override fun toString(): String = classInfo.toString()

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmType -> false
        classInfo != other.classInfo -> false
        else -> true
    }

    override fun hashCode(): Int = classInfo.hashCode()

    companion object {
        val OBJECT = native("java.lang.Object")
        val STRING = native("java.lang.String")
        val BOOLEAN = native("java.lang.Boolean")

        private fun native(name: String): JvmType =
            JvmType(scanForClass(name) ?: error("Could not find native Java class '$name'"))
    }
}