package net.ormr.jukkas

import net.ormr.jukkas.type.ResolvedType
import net.ormr.jukkas.type.TypeResolver

class CompilerContext(val typeResolvers: List<TypeResolver>) {
    fun resolveType(path: String, symbol: String): ResolvedType? =
        typeResolvers.firstNotNullOfOrNull { it.resolve(path, symbol) }
}