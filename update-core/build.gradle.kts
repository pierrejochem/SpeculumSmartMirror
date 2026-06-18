import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared update logic (GitHub releases, version compare, asset selection,
// signature verification) used by BOTH the config-server (to apply updates) and
// the updatenotifier plugin (to notify). Deliberately NOT published — it stays
// out of the :mirror-api SDK POM so external module authors don't inherit the
// ktor-client / BouncyCastle surface.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // `api` so consumers (config-server) get the ktor client + serialization too.
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.ktor.client.core)
    api(libs.ktor.client.contentnegotiation)
    api(libs.ktor.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.bouncycastle.pg)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.client.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") { useJUnitPlatform() }
