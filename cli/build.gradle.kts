plugins {
    application
}

repositories {
    maven(url = "https://jitpack.io")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("com.github.AharonSambol:PrettyPrintTreeJava:b32d1bab64")
    implementation("com.github.ajalt.clikt:clikt:4.0.0.198-SNAPSHOT")
    implementation(project(":compiler"))
}

application {
    mainClass.set("net.ormr.jukkas.cli.MainKt")
}