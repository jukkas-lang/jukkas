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

import net.ormr.jukkas.type.member.JvmMember
import net.ormr.jukkas.type.member.TypeMember
import net.ormr.jukkas.utils.getDescriptor
import net.ormr.krautils.collections.asUnmodifiableList
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

class JvmReferenceType private constructor(val clz: Class<*>) : JvmType {
    override val superType: ResolvedType? by lazy { clz.superclass?.let { of(it) } }

    override val interfaces: List<ResolvedType> by lazy {
        clz.interfaces.map { of(it) }.asUnmodifiableList()
    }

    override val internalName: String = JvmTypeResolver.buildJukkasName(clz.packageName, clz.simpleName)

    // empty if located in root package
    override val packageName: String = clz.packageName.replace('.', '/')

    // TODO: empty if class is anonymous
    override val simpleName: String = clz.simpleName.replace('$', '.')

    override val members: List<TypeMember> by lazy {
        createMemberList(clz.methods, clz.constructors, clz.fields)
    }

    override val declaredMembers: List<JvmMember> by lazy {
        createMemberList(clz.declaredMethods, clz.declaredConstructors, clz.declaredFields)
    }

    override val isInterface: Boolean
        get() = clz.isInterface

    // TODO: we need have a smarter strategy for resolving overloads
    override fun findMethod(name: String, types: List<ResolvedTypeOrError>): TypeMember.Method? {
        walkHierarchy { type ->
            type.declaredMembers
                .asSequence()
                .filterIsInstance<TypeMember.Method>()
                .filter { it.name == name }
                .filter { argumentsMatch(it, types) }
                .sortedWith(methodComparator)
                .firstOrNull()
                ?.let { return it }
        }
        return null
    }

    override fun findMethods(name: String): List<TypeMember.Method> = buildList {
        walkHierarchy { type ->
            for (member in type.declaredMembers) {
                if (member is TypeMember.Method && member.name == name) {
                    add(member)
                }
            }
        }
    }

    override fun findConstructor(types: List<ResolvedTypeOrError>): JvmMember.Constructor? =
        findDeclaredMember { typesMatch(types, it.parameterTypes) }

    // TODO: handle varargs and static functions?
    private fun argumentsMatch(
        method: TypeMember.Method,
        types: List<ResolvedTypeOrError>,
    ): Boolean = typesMatch(types, method.parameterTypes)

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
            is JvmArrayType -> false
            is JvmPrimitiveType -> this == other.boxedType
            is JvmReferenceType -> other isAssignableFrom this // TODO: is this the right order?
        }
    }

    override fun compareCompatibility(other: ResolvedTypeOrError): Int = when (other) {
        is ErrorType -> 0
        is JukkasType -> TODO("JukkasType")
        is JvmType -> when (other) {
            is JvmArrayType -> 0
            is JvmPrimitiveType -> compareCompatibility(other.boxedType)
            is JvmReferenceType -> when {
                // if 'this' is the parent of 'other' then this > other
                this isAssignableFrom other -> 1
                // if 'other' is the parent of 'this' then this < other
                other isAssignableFrom this -> -1
                else -> 0
            }
        }
    }

    private infix fun isAssignableFrom(other: JvmReferenceType): Boolean = clz.isAssignableFrom(other.clz)

    override fun toJvmDescriptor(): String = getDescriptor(clz)

    override fun toAsmType(): AsmReferenceType = AsmReferenceType.of(clz)

    override fun toString(): String = internalName

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is JvmReferenceType -> false
        clz != other.clz -> false
        else -> true
    }

    override fun hashCode(): Int = clz.hashCode()

    companion object {
        private val methodComparator = Comparator<TypeMember.Method> { o1, o2 ->
            // TODO: support non JVM methods
            require(o1 is JvmMember.Method) { "Only JVM methods allowed for now" }
            require(o2 is JvmMember.Method) { "Only JVM methods allowed for now" }
            when {
                o1.method.isVarArgs && !(o2.method.isVarArgs) -> 1
                // TODO: should it be 'o1' or 'o2' here
                o2.method.isVarArgs && !(o1.method.isVarArgs) -> -1
                else -> compareTypes(o1.parameterTypes, o2.parameterTypes)
            }

        }
        private val cache = hashMapOf<String, JvmReferenceType>()

        val OBJECT: JvmReferenceType = of<Any>()
        val STRING: JvmReferenceType = of<String>()
        val CHAR: JvmReferenceType = of<Char>()
        val BOOLEAN: JvmReferenceType = of<Boolean>()
        val VOID: JvmReferenceType = of<Void>()
        val BYTE: JvmReferenceType = of<Byte>()
        val SHORT: JvmReferenceType = of<Short>()
        val INT: JvmReferenceType = of<Int>()
        val LONG: JvmReferenceType = of<Long>()
        val FLOAT: JvmReferenceType = of<Float>()
        val DOUBLE: JvmReferenceType = of<Double>()

        private inline fun <reified T : Any> of(): JvmReferenceType = of(T::class.javaObjectType)

        // TODO: 'info.name' is probably not correct as we want to use the full name?
        fun of(clz: Class<*>): JvmReferenceType {
            require(!clz.isPrimitive) { "Class <$clz> is a primitive, use JvmPrimitiveType instead" }
            require(!clz.isArray) { "Class <$clz> is an array, use JvmArrayType instead" }
            return cache.getOrPut(getDescriptor(clz)) { JvmReferenceType(clz) }
        }

        /**
         * Returns a [JvmReferenceType] for a [Class] instance loaded from the given [name], or `null` if no class
         * with `name` could be loaded.
         *
         * This uses the [Class.forName] function for loading classes.
         *
         * @see [Class.forName]
         */
        fun find(name: String): JvmReferenceType? {
            val clz = try {
                // TODO: this will initialize the class, do we want that?
                Class.forName(name)
            } catch (_: Exception) {
                return null
            }
            return of(clz)
        }

        private fun compareTypes(a: List<ResolvedTypeOrError>, b: List<ResolvedTypeOrError>): Int {
            when {
                a.size != b.size -> return a.size.compareTo(b.size)
                else -> {
                    for (i in a.indices) {
                        val cmp = a[i].compareCompatibility(b[i])

                        if (cmp != 0) {
                            return cmp
                        }
                    }

                    return 0
                }
            }
        }
    }
}

private fun createMemberList(
    methods: Array<Method>,
    constructors: Array<Constructor<*>>,
    fields: Array<Field>,
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