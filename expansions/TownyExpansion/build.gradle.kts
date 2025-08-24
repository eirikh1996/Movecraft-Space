plugins {
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow")
    kotlin("jvm")
}

tasks.register<Wrapper>("wrapper") {
    gradleVersion = "8.12.1"
}

tasks.register("prepareKotlinBuildScriptModel") {

}

dependencies {
    compileOnly(project(":movecraft-space-api"))
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.2.0")
}
repositories {
    mavenCentral()
    maven("https://repo.glaremasters.me/repository/towny/")
}

tasks.shadowJar {
    archiveBaseName.set("TownyExpansion")
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