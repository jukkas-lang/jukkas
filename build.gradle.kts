group = "net.ormr.jukkas"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks {
    register("runDetekt") {
        finalizedBy(
            project(":compiler").tasks.getByPath("detekt"),
            project(":cli").tasks.getByPath("detekt"),
        )
    }
}