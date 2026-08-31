plugins {
    kotlin("jvm") version "1.9.24"
    id("com.gradleup.shadow") version "8.3.3"
}

group = "com.tyler.ggvsgoons"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("GGvGoons-${project.version}.jar")
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
