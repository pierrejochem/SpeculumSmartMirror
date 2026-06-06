import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
}

// Published coordinates: org.speculum:mirror-api:<version>. The version comes
// from `-PreleaseVersion=…` (the release workflow passes the git tag without its
// leading "v"), else the VERSION env, else a local SNAPSHOT.
group = "org.speculum"
version = (findProperty("releaseVersion") as String?)?.removePrefix("v")
    ?: System.getenv("VERSION")?.removePrefix("v")
    ?: "0.0.0-SNAPSHOT"

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    api(compose.runtime)
    api(compose.ui)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") { useJUnitPlatform() }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "mirror-api"
            pom {
                name.set("Speculum mirror-api")
                description.set("Plugin/module API for the Speculum smart-mirror dashboard.")
            }
        }
    }
    repositories {
        // GitHub Packages. URL + credentials are taken from the Actions
        // environment at publish time (GITHUB_REPOSITORY = "owner/repo",
        // GITHUB_TOKEN provided by the workflow). For local publishing, set
        // GITHUB_REPOSITORY + a PAT with write:packages in GPR_TOKEN/GITHUB_TOKEN.
        maven {
            name = "GitHubPackages"
            url = uri(
                "https://maven.pkg.github.com/" +
                    (System.getenv("GITHUB_REPOSITORY") ?: "pierrejochem/Speculum")
            )
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: System.getenv("GPR_USER")
                password = System.getenv("GITHUB_TOKEN") ?: System.getenv("GPR_TOKEN")
            }
        }
    }
}
