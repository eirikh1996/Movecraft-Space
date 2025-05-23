plugins {
    id("buildlogic.java-conventions")
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

group = "io.github.eirikh1996"

repositories {
    maven("https://repo.mikeprimm.com/")
    mavenCentral()
}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly("org.dynmap:DynmapCoreAPI:2.0")
    compileOnly(kotlin("stdlib-jdk8"))
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}