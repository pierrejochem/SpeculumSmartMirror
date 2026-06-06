plugins {
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

/** Build every module JAR and copy it into the `plugins/` folder. */
tasks.register("deployModules") {
    description = "Builds all default modules and copies their JARs into the app's `plugins/` folder for runtime loading."
    dependsOn(
        ":modules:example-module:deployToModules",
        ":modules:clock-module:deployToModules",
        ":modules:weather-module:deployToModules",
        ":modules:calendar-module:deployToModules",
        ":modules:compliments-module:deployToModules",
        ":modules:newsfeed-module:deployToModules",
        ":modules:qr-module:deployToModules",
    )
}
