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

import net.ormr.jukkas.type.member.JavaConstructor
import net.ormr.jukkas.type.member.JavaField
import net.ormr.jukkas.type.member.JavaMethod
import net.ormr.jukkas.type.member.JvmConstructor
import net.ormr.jukkas.type.member.JvmField
import net.ormr.jukkas.type.member.JvmMethod
import net.ormr.jukkas.type.member.TypeMember

class JvmReferenceType internal constructor(override val clz: Class<*>) : JvmType {
    init {
        require(!clz.isPrimitive) { "Class <$clz> is a primitive, use JvmPrimitiveType instead" }
        require(!clz.isArray) { "Class <$clz> is an array, use JvmArrayType instead" }
    }

    override val qualifiedName: String = JvmTypeResolver.toJukkasName(clz.name)
    override val simpleName: String
        get() = clz.simpleName // TODO: convert to jukkas style
    override val superType: ContainerType? by lazy { clz.superclass?.let { JvmType.of(it) } }
    override val interfaces: List<ContainerType> by lazy { clz.interfaces.map { JvmType.of(it) } }
    override val members: List<TypeMember> by lazy {
        createMemberList(clz.methods, clz.constructors, clz.fields)
    }
    override val declaredMembers: List<TypeMember> by lazy {
        createMemberList(clz.declaredMethods, clz.declaredConstructors, clz.declaredFields)
    }

    override val isObject: Boolean
        get() = false
    override val isInterface: Boolean
        get() = clz.isInterface

    override fun isCompatibleWith(other: Type): Boolean {
        if (this isSameType other) return true
        return when (other) {
            is JvmType -> when (other) {
                is JvmArrayType -> false
                // TODO: support auto boxing and stuff
                is JvmPrimitiveType -> this isCompatibleWith other.boxedType
                is JukkasJvmType, is JvmReferenceType -> super.isCompatibleWith(other)
            }
            else -> super.isCompatibleWith(other)
        }
    }

    override fun compareCompatibilityTo(other: Type): Int = when (other) {
        is JvmType -> when (other) {
            is JvmArrayType -> 0
            is JvmPrimitiveType -> this compareCompatibilityTo other.boxedType
            is JukkasJvmType, is JvmReferenceType -> super.compareCompatibilityTo(other)
        }
        else -> super.compareCompatibilityTo(other)
    }

    // TODO: properly support properties
    override fun findProperty(name: String): TypeMember.Property? = findMember<JvmField> { it.name == name }

    override fun asString(): String = qualifiedName

    private val cachedAsmType by lazy { AsmReferenceType.of(clz) }

    override fun toAsmType(): AsmFieldType = cachedAsmType

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmReferenceType -> false
        clz != other.clz -> false
        else -> true
    }

    override fun hashCode(): Int = clz.hashCode()

    override fun toString(): String = "JvmReferenceType(clz=$clz)"
}

// TODO: populate JvmProperty instances with potential functions that can be used as properties
// TODO: priority order when resolving is:
//       - if field exists with name, check visibility
//       - if visibility is lower then what we can access check for getters
private fun createMemberList(
    methods: Array<JavaMethod>,
    constructors: Array<JavaConstructor<*>>,
    fields: Array<JavaField>,
): List<TypeMember> = buildList(methods.size + constructors.size + fields.size) {
    for (method in methods) {
        add(JvmMethod(method))
    }

    for (constructor in constructors) {
        add(JvmConstructor(constructor))
    }

    for (field in fields) {
        add(JvmField(field))
    }
}