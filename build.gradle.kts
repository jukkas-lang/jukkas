import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA

plugins {
    id("com.adarshr.test-logger") version "3.2.0"
}

group = "net.ormr.jukkas"
version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "com.adarshr.test-logger")

    testlogger {
        theme = MOCHA
        showSummary = true
        showOnlySlow = false
    }

    repositories {
        mavenCentral()
    }
}

tasks {
    register("runDetekt") {
        finalizedBy(
            project(":compiler").tasks.getByPath("detekt"),
        )
    }
}