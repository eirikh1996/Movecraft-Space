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
    compileOnly(files(
        "/../../libs/Factions 3.2.3/MassiveCore.jar",
        "/../../libs/Factions 3.2.3/Factions.jar"))
}
repositories {
    mavenCentral()
}

tasks.shadowJar {
    archiveBaseName.set("FactionsExpansion")
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