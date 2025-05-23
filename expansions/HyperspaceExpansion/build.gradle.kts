plugins {
    id("buildlogic.java-conventions")
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("io.github.0ffz.github-packages") version "1.2.1"
}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly(kotlin("stdlib-jdk8"))
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    compileOnly("net.countercraft:movecraft:+")
}
repositories {
    mavenCentral()
    maven { githubPackage("apdevteam/movecraft")(this) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}