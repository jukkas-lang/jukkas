package net.ormr.jukkas

import net.ormr.jukkas.type.BuiltinTypes
import net.ormr.jukkas.type.Type
import net.ormr.jukkas.type.TypeResolver

class CompilerContext(val typeResolvers: List<TypeResolver>, val builtinTypes: BuiltinTypes) {
    fun resolveType(path: String, symbol: String): Type? =
        typeResolvers.firstNotNullOfOrNull { it.resolve(path, symbol) }
}