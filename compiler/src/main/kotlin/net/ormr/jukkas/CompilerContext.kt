package net.ormr.jukkas

import net.ormr.jukkas.newtype.TypeResolver
import net.ormr.jukkas.newtype.Type


class CompilerContext(val typeResolvers: List<TypeResolver>) {
    fun resolveType(path: String, symbol: String): Type? =
        typeResolvers.firstNotNullOfOrNull { it.resolve(path, symbol) }
}