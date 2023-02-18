package net.ormr.jukkas

import net.ormr.jukkas.type.TypeResolver

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CompilerContextBuilderDsl

@CompilerContextBuilderDsl
class CompilerContextBuilder {
    var resolverBuilder: TypeResolverBuilder? = null

    @CompilerContextBuilderDsl
    inner class TypeResolverBuilder {
        val typeResolvers = mutableListOf<TypeResolver>()

        fun resolver(resolver: TypeResolver) {
            typeResolvers.add(resolver)
        }
    }

    fun types(builder: TypeResolverBuilder.() -> Unit) {
        require(resolverBuilder == null) { "'types' should only be declared once" }
        val resolverBuilder = TypeResolverBuilder()
        builder.invoke(resolverBuilder)
        this.resolverBuilder = resolverBuilder
    }

    fun build(): CompilerContext {
        require(resolverBuilder != null) { "'types' should be declared" }
        return CompilerContext(
            resolverBuilder!!.typeResolvers
        )
    }
}

fun buildCompilationContext(builder: CompilerContextBuilder.() -> Unit): CompilerContext {
    val compilerContextBuilder = CompilerContextBuilder()
    builder.invoke(compilerContextBuilder)
    return compilerContextBuilder.build()
}