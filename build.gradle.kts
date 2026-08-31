plugins {
    kotlin("jvm") version "1.9.24"
    id("com.gradleup.shadow") version "8.1.1"
}

group = "com.tyler.ggvg"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io") // CommandAPI
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shaded:9.5.3")
}

kotlin {
    jvmToolchain(17)
}

task.shadowJar {
    archiveFileName.set("GGvGoon-${version }.jar")
    relocate("dev.jorel.commandapi", "com.tyler.ggvg.commandapi")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}