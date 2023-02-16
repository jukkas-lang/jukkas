plugins {
    application
    kotlin("plugin.serialization") version "1.8.0"
}

repositories {
    maven(url = "https://jitpack.io")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.0.0.198-SNAPSHOT")
    implementation(project(":compiler"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
}

application {
    mainClass.set("net.ormr.jukkas.cli.MainKt")
}