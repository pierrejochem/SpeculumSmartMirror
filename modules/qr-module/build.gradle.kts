import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Provided by the host app at runtime (shared via the parent classloader).
    compileOnly(project(":mirror-api"))
    compileOnly(compose.runtime)
    compileOnly(compose.foundation)
    compileOnly(compose.material3)
    compileOnly(compose.ui)
    compileOnly(libs.zxing.core)
}

val deployToModules by tasks.registering(Copy::class) {
    from(tasks.named("jar"))
    into(rootProject.layout.projectDirectory.dir("plugins"))
}