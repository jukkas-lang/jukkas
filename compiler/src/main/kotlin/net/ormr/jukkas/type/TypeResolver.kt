package net.ormr.jukkas.type

interface TypeResolver {
    fun resolve(path: String, symbol: String): ResolvedType?
}