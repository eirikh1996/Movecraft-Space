plugins {
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow")
    kotlin("jvm")
}

group = "io.github.eirikh1996"

repositories {
    maven("https://repo.mikeprimm.com/")
    mavenCentral()
}
//project.properties.get()
dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly(project(":HyperspaceExpansion"))
    compileOnly("org.dynmap:DynmapCoreAPI:2.0")
    compileOnly(kotlin("stdlib-jdk8"))
}

tasks.shadowJar {
    archiveBaseName.set("DynmapExpansion")
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