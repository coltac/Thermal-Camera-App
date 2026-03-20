pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "UVC Thermal"
include(":app")
include(":libausbc")
include(":libuvc")
include(":libnative")

project(":libausbc").projectDir = file(".deps/AndroidUSBCamera/libausbc")
project(":libuvc").projectDir = file(".deps/AndroidUSBCamera/libuvc")
project(":libnative").projectDir = file(".deps/AndroidUSBCamera/libnative")
 
