plugins {
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.9-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9-SNAPSHOT")
    compileOnly(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo/")
}

tasks.shadowJar {
    archiveBaseName.set("WorldGuardExpansion")
    archiveClassifier.set("")
    archiveVersion.set("")
    destinationDirectory.set(file("/../../plugin/build/libs/expansions"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}