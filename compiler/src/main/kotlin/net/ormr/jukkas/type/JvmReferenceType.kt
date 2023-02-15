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

import io.github.classgraph.ArrayClassInfo
import io.github.classgraph.ClassInfo
import io.github.classgraph.FieldInfoList
import io.github.classgraph.MethodInfoList
import net.ormr.jukkas.type.member.JvmMember
import net.ormr.jukkas.type.member.TypeMember
import net.ormr.jukkas.utils.scanForClass
import net.ormr.krautils.collections.asUnmodifiableList

class JvmReferenceType private constructor(val classInfo: ClassInfo) : JvmType {
    override val superType: ResolvedType? by lazy { classInfo.superclass?.let { from(it) } }

    override val interfaces: List<ResolvedType> by lazy {
        classInfo.interfaces.map { from(it) }.asUnmodifiableList()
    }

    override val internalName: String = "${packageName.replace('.', '/')}/${simpleName.replace('$', '.')}"

    // empty if located in root package
    override val packageName: String
        get() = classInfo.packageName

    override val simpleName: String
        get() = classInfo.simpleName

    override val members: List<TypeMember> by lazy {
        createMemberList(classInfo.methodInfo, classInfo.constructorInfo, classInfo.fieldInfo)
    }

    override val declaredMembers: List<JvmMember> by lazy {
        createMemberList(classInfo.declaredMethodInfo, classInfo.declaredConstructorInfo, classInfo.declaredFieldInfo)
    }

    override fun findMethod(name: String, types: List<ResolvedType>): JvmMember.Method? =
        findMember { it.name == name && typesMatch(types, it.parameterTypes) }

    override fun findConstructor(types: List<ResolvedType>): JvmMember.Constructor? =
        findDeclaredMember { typesMatch(types, it.parameterTypes) }

    private fun typesMatch(a: List<ResolvedTypeOrError>, b: List<ResolvedTypeOrError>): Boolean {
        if (a.size != b.size) return false

        for (i in a.indices) {
            if (a[i] isIncompatible b[i]) {
                return false
            }
        }

        return true
    }

    override fun findField(name: String): JvmMember.Field? = findMember { it.name == name }

    override fun isCompatible(other: ResolvedTypeOrError): Boolean = when (other) {
        is ErrorType -> false
        is JukkasType -> TODO("isCompatible -> JukkasType")
        // TODO: is this sound?
        is JvmType -> when (other) {
            is JvmPrimitiveType -> false // a wrapper type is never allowed in place of a primitive
            is JvmReferenceType -> this sameJvmDescriptor other || this extendsOrImplements other
        }
    }

    infix fun extendsOrImplements(other: JvmReferenceType): Boolean =
        classInfo.extendsSuperclass(other.classInfo.name) || classInfo.implementsInterface(other.classInfo.name)

    override fun toJvmDescriptor(): String = when (classInfo) {
        is ArrayClassInfo -> classInfo.typeSignatureStr
        else -> "L$jvmName;"
    }

    override fun toAsmType(): AsmType = when (classInfo) {
        is ArrayClassInfo -> AsmArrayType.fromDescriptor(classInfo.typeSignatureStr)
        else -> AsmReferenceType.fromDescriptor(toJvmDescriptor())
    }

    override fun toString(): String = classInfo.toString()

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmReferenceType -> false
        classInfo != other.classInfo -> false
        else -> true
    }

    override fun hashCode(): Int = classInfo.hashCode()

    private fun createMemberList(
        methods: MethodInfoList,
        constructors: MethodInfoList,
        fields: FieldInfoList,
    ): List<JvmMember> = buildList(methods.size + constructors.size + fields.size) {
        for (method in methods) {
            add(JvmMember.Method(method))
        }

        for (constructor in constructors) {
            add(JvmMember.Constructor(constructor))
        }

        for (field in fields) {
            add(JvmMember.Field(field))
        }
    }

    companion object {
        private val cache = hashMapOf<String, JvmReferenceType>()

        // TODO: switch away from ClassGraph, it explicitly removes java.lang.Object which makes stuff a fucking pita
        val OBJECT: JvmReferenceType = of("java.lang.Object")
        val STRING: JvmReferenceType = of("java.lang.String")
        val INT: JvmReferenceType = of("java.lang.Integer")
        val BOOLEAN: JvmReferenceType = of("java.lang.Boolean")

        // TODO: 'info.name' is probably not correct as we want to use the full name?
        fun from(info: ClassInfo): JvmReferenceType = cache.getOrPut(info.name) { JvmReferenceType(info) }

        fun of(name: String): JvmReferenceType = cache.getOrPut(name) {
            val info = scanForClass(name)
            require(info != null) { "Could not find class: $name" }
            JvmReferenceType(info)
        }
    }
}