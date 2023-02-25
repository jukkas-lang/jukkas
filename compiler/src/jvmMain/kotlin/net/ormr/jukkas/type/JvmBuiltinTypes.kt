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

import kotlin.reflect.typeOf

internal object JvmBuiltinTypes : BuiltinTypes {
    override val any: ContainerType = reference<Any>()
    override val nothing: ContainerType
        get() = TODO("Builtin type for 'Nothing'")
    override val unit: ContainerType = primitive<Void>() // TODO: is this sound?
    override val string: ContainerType = reference<String>()

    // TODO: we should cast the primitives to our own wrapper types in some manner
    override val boolean: ContainerType = primitive<Boolean>()
    override val char: ContainerType = primitive<Char>()
    override val byte: ContainerType = primitive<Byte>()
    override val short: ContainerType = primitive<Short>()
    override val int: ContainerType = primitive<Int>()
    override val long: ContainerType = primitive<Long>()
    override val float: ContainerType = primitive<Float>()
    override val double: ContainerType = primitive<Double>()

    private inline fun <reified T : Any> reference(): JvmType = JvmType.of(T::class.javaObjectType)

    private inline fun <reified T : Any> primitive(): JvmType =
        JvmType.of(T::class.javaPrimitiveType ?: error("Type <${typeOf<T>()}> is not a primitive"))
}