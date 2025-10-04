plugins {
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow")
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

group = "io.github.eirikh1996"

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly(kotlin("stdlib-jdk8"))
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.shadowJar {
    archiveBaseName.set("AsteroidBeltExpansion")
    archiveClassifier.set("")
    archiveVersion.set("")
    destinationDirectory.set(file("/../../plugin/build/libs/expansions"))
}

kotlin {
    jvmToolchain(21)
}