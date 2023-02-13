plugins {
    id("org.xbib.gradle.plugin.jflex") version "1.4.0"
}

dependencies {
    //implementation("org.ow2.asm:asm-commons:9.4")
    implementation("io.github.classgraph:classgraph:4.8.154")
    implementation("net.ormr.asmkt:asmkt:0.0.9")
    implementation(kotlin("reflect"))
}

sourceSets {
    main {
        jflex {
            include("**/*.flex")
        }
        java {
            srcDir("$buildDir/generated/jflex")
        }
    }
}

tasks {
    compileKotlin {
        dependsOn(named("generateJflex"))
    }
}