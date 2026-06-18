import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":mirror-api"))
    implementation(project(":update-core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentnegotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.statuspages)
    implementation(libs.ktor.server.calllogging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback.classic)
}

application {
    mainClass = "org.speculum.configserver.ServerKt"
}

// Run from the repo root so it shares config.json + plugins/ with the desktop app
// and finds the built web UI at config-server/web/dist.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    // Surface the shared version for the admin's read-only display (GET /api/version).
    // In the packaged app the server is embedded in the composeApp JVM, which
    // already sets this property; standalone runs get it from gradle.properties.
    jvmArgs("-Dspeculum.version=${providers.gradleProperty("speculum.version").getOrElse("dev")}")
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") { useJUnitPlatform() }
