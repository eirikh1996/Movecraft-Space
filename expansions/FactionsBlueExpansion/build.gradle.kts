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
    compileOnly(files("/../../libs/FactionsBlue-1.1.6-STABLE.jar"))
}
repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.shadowJar {
    archiveBaseName.set("FactionsBlueExpansion")
    archiveClassifier.set("")
    archiveVersion.set("")
    destinationDirectory.set(file("/../../plugin/build/libs/expansions"))
}

kotlin {
    jvmToolchain(21)
}