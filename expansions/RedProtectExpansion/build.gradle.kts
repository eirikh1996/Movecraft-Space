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
    compileOnly(files("/../../libs/RedProtect-8.1.2-SNAPSHOT-b444-Spigot.jar"))
}
repositories {
    mavenCentral()
}

tasks.shadowJar {
    archiveBaseName.set("RedProtectExpansion")
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