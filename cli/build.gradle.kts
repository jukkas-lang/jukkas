import io.gitlab.arturbosch.detekt.report.ReportMergeTask

plugins {
    kotlin("jvm") version "1.8.20-Beta"
    kotlin("plugin.serialization") version "1.8.20-Beta"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    application
}

repositories {
    maven(url = "https://jitpack.io")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("net.ormr.krautils:krautils-core:0.2.0")
    implementation("com.github.ajalt.clikt:clikt:4.0.0.198-SNAPSHOT")
    implementation(project(":compiler"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
}

application {
    mainClass.set("net.ormr.jukkas.cli.MainKt")
}

detekt {
    config = rootProject.files("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

tasks {
    val reportMerge by registering(ReportMergeTask::class) {
        output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
        input.from(detekt.get().sarifReportFile)
    }

    detekt.configure {
        finalizedBy(reportMerge)

        reports {
            sarif.required.set(true)
            sarif.outputLocation.set(file("build/reports/detekt.sarif"))
        }

        basePath = rootProject.projectDir.absolutePath
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}