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
    // The host app provides these at runtime (shared via the parent classloader),
    // so they are compile-only and the JAR stays thin.
    compileOnly(project(":mirror-api"))
    compileOnly(libs.compose.runtime)
    compileOnly(libs.compose.foundation)
    compileOnly(libs.compose.material3)
    compileOnly(libs.kotlinx.coroutines.core)
}

// Drop the built JAR into the app's `modules/` folder so it gets picked up.
val deployToModules by tasks.registering(Copy::class) {
    from(tasks.named("jar"))
    into(rootProject.layout.projectDirectory.dir("plugins"))
}