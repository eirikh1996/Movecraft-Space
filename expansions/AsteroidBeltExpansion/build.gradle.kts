plugins {
    id("buildlogic.java-conventions")
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

kotlin {
    jvmToolchain(21)
}