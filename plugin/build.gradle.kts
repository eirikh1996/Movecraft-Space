plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev")
    id("io.github.0ffz.github-packages") version "1.2.1"
    id("com.gradleup.shadow")
    kotlin("jvm")
}

repositories {

    maven { githubPackage("apdevteam/movecraft")(this) }
    mavenCentral()
}

description = "Movecraft-Space"

dependencies {
    implementation(project(":movecraft-space-api"))
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.countercraft:movecraft:+")
    implementation(kotlin("stdlib-jdk8"))
}

java {
}
kotlin {
    jvmToolchain(23)
}

tasks.shadowJar {
    archiveBaseName.set("Movecraft-Space")
    archiveClassifier.set("")
    archiveVersion.set("")
    dependencies {
        include(project(":movecraft-space-api"))
    }
}

tasks.processResources {
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}