plugins {
    id("buildlogic.java-conventions")
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.9-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9-SNAPSHOT")
    compileOnly(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}