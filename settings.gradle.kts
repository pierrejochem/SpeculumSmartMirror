rootProject.name = "Speculum"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
include(":mirror-api")
include(":config-server")
include(":update-core")
include(":modules:example-module")
include(":modules:clock-module")
include(":modules:weather-module")
include(":modules:calendar-module")
include(":modules:compliments-module")
include(":modules:newsfeed-module")
include(":modules:qr-module")
include(":modules:updatenotifier-module")
