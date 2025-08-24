plugins {
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("com.github.Angeschossen:LandsAPI:7.15.20")
}
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

tasks.shadowJar {
    archiveBaseName.set("LandsExpansion")
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