plugins {
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow")
    kotlin("jvm")
    //id("io.github.0ffz.github-packages") version "1.2.1"
}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}

tasks.shadowJar {
    archiveBaseName.set("HyperspaceExpansion")
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