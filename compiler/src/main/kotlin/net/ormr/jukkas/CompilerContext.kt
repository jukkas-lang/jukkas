package net.ormr.jukkas

import net.ormr.jukkas.type.BuiltinTypes
import net.ormr.jukkas.type.Type

interface CompilerContext {
    val builtinTypes: BuiltinTypes

    fun resolveType(path: String, symbol: String): Type?
}