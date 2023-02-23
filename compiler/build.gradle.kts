import io.gitlab.arturbosch.detekt.report.ReportMergeTask

plugins {
    kotlin("multiplatform") version "1.8.20-Beta"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("io.kotest.multiplatform") version "5.5.5"
}

group = "net.ormr.jukkas"
version = "1.0-SNAPSHOT"

val kotestVersion: String by rootProject

@Suppress("UnusedPrivateMember")
kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
            withJava()
            testRuns["test"].executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js(IR) {
        browser()
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.contracts.ExperimentalContracts")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                implementation("io.kotest:kotest-property:$kotestVersion")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("net.ormr.asmkt:asmkt:0.0.9")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
            }
        }
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
}

detekt {
    config = rootProject.files("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    // hacky way of making the 'detekt' task run for all the multiplatform sources, as Detekt has no inbuilt
    // task for accomplishing this
    val srcDirs = kotlin.sourceSets.flatMap { it.kotlin.sourceDirectories.files }.toTypedArray()
    @Suppress("SpreadOperator")
    source.from(*srcDirs)
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
}
