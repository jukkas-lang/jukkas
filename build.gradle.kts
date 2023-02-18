import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.adarshr.test-logger") version "3.2.0"
}

group = "net.ormr.jukkas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotestVersion: String by project

testlogger {
    theme = MOCHA
    showSummary = true
    showOnlySlow = false
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.adarshr.test-logger")

    repositories {
        mavenCentral()
    }

    testlogger {
        theme = MOCHA
        showSummary = true
        showOnlySlow = false
    }

    dependencies {
        implementation("net.ormr.krautils:krautils-core:0.2.0")

        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
    }

    tasks {
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