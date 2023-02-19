import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.adarshr.test-logger") version "3.2.0"
    id("io.gitlab.arturbosch.detekt").version("1.22.0")
}

group = "net.ormr.jukkas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotestVersion: String by project

allprojects {
    apply(plugin = "com.adarshr.test-logger")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    testlogger {
        theme = MOCHA
        showSummary = true
        showOnlySlow = false
    }

    detekt {
        config = rootProject.files("config/detekt/detekt.yml")
        buildUponDefaultConfig = true
    }

    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
    }

    tasks {
        detekt.configure {
            basePath = rootProject.projectDir.absolutePath
        }
    }
}

val reportMerge by tasks.registering(ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.adarshr.test-logger")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("net.ormr.krautils:krautils-core:0.2.0")

        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
    }

    tasks {
        detekt.configure {
            finalizedBy(reportMerge)

            reportMerge.configure {
                input.from(sarifReportFile)
            }

            reports {
                sarif.required.set(true)
                sarif.outputLocation.set(file("build/reports/detekt.sarif"))
            }
        }

        compileKotlin {
            compilerOptions {
                jvmTarget.set(JVM_17)
                freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
            }
        }

        compileTestKotlin {
            compilerOptions {
                jvmTarget.set(JVM_17)
            }
        }

        test {
            useJUnitPlatform()
        }
    }
}