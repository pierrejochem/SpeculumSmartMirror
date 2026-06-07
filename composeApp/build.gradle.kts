import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
    implementation(project(":mirror-api"))
    implementation(project(":config-server")) // embed the web admin in-process
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentnegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.zxing.core) // QR encoding (provided to the qr-module plugin at runtime)
}

// Single source of the app version: the package version, and (via a system
// property) what the update-notifier module compares against the latest release.
val appVersion = "1.0.0"

// Module JARs are bundled into the package here; at runtime the app finds them
// via `compose.application.resources.dir` (see PluginLoader).
val packagedPluginsDir = layout.projectDirectory.dir("app-resources/common/plugins")

compose.desktop {
    application {
        mainClass = "org.speculum.MainKt"

        nativeDistributions {
            // Deb for Raspberry Pi OS / Debian. Build ON the target arch
            // (jpackage cannot cross-compile) — see PACKAGING.md.
            targetFormats(TargetFormat.Deb)
            packageName = "speculum"
            packageVersion = appVersion
            description = "Speculum — modular smart-mirror dashboard"
            vendor = "Speculum"

            // Bundle a full JRE so the Pi needs no separate Java install.
            includeAllModules = true

            // Low-memory tuning for small devices (e.g. Raspberry Pi, 1 GB RAM):
            // cap the heap and use the low-overhead serial GC. Live heap is ~45 MB,
            // so 160 MB is ample and keeps RSS well under the default ¼-of-RAM.
            // `speculum.version` lets the update-notifier module read the running
            // version without any config (the single source is `appVersion`).
            jvmArgs += listOf(
                "-Xmx160m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=96m",
                "-Dspeculum.version=$appVersion",
            )

            // Ship the module plugin JARs inside the package; exposed at runtime
            // under <resources.dir>/plugins.
            appResourcesRootDir.set(layout.projectDirectory.dir("app-resources"))

            linux {
                packageName = "speculum"
                menuGroup = "Speculum"
                appCategory = "Utility"
                iconFile.set(layout.projectDirectory.file("icons/speculum.png"))
            }
        }
    }
}

// Build + deploy external module JARs into the `plugins/` folder before running,
// and run from the project root so the app finds that `plugins/` folder.
tasks.matching { it.name == "run" }.configureEach {
    dependsOn(":deployModules")
    // The update-notifier detects dev runs by the absence of the jpackage
    // resources dir, so no version property is needed here.
    (this as? JavaExec)?.workingDir = rootProject.projectDir
}

// Copy the built module JARs into the package's app-resources so they ship
// inside the .deb. The packaging tasks depend on this.
val bundlePlugins by tasks.registering(Copy::class) {
    dependsOn(":deployModules")
    from(rootProject.layout.projectDirectory.dir("plugins")) { include("*.jar") }
    into(packagedPluginsDir)
}

// Bundle the built web admin UI (run `npm run build` in config-server/web first)
// so the embedded server can serve it from the packaged resources.
val bundleWeb by tasks.registering(Copy::class) {
    from(project(":config-server").projectDir.resolve("web/dist"))
    into(layout.projectDirectory.dir("app-resources/common/web"))
}

tasks.matching { it.name == "prepareAppResources" }
    .configureEach { dependsOn(bundlePlugins, bundleWeb) }

compose.resources {
    publicResClass = true
    packageOfResClass = "org.speculum.resources"
    generateResClass = always
}