package net.ormr.jukkas.type

object JvmTypeResolver : TypeResolver {
    override fun resolve(path: String, symbol: String): ResolvedType? {
        return JvmReferenceType.find(buildJavaName(path, symbol))
    }

    /**
     * Converts a Jukkas type name *(eq; `foo/bar/Foo.Bar`)* to a Java class name *(`foo.bar.Foo$Bar`)*.
     *
     * @see [toJukkasName]
     */
    fun toJavaName(jukkasName: String): String = jukkasName.replace('.', '$').replace('/', '.')

    fun buildJavaName(packageName: String, className: String): String {
        val pkg = packageName.replace('/', '.')
        return "$pkg${if (pkg.isNotEmpty()) "." else ""}${className.replace('.', '$')}"
    }

    /**
     * Converts a Java type name *(eq; `foo.bar.Foo$Bar`)* to a Jukkas class name *(`foo/bar/Foo.Bar`)*.
     *
     * @see [toJavaName]
     */
    fun toJukkasName(javaName: String): String = javaName.replace('.', '/').replace('$', '.')

    fun buildJukkasName(packageName: String, className: String): String {
        val pkg = packageName.replace('.', '/')
        return "$pkg${if (pkg.isNotEmpty()) "/" else ""}${className.replace('$', '.')}"
    }
}