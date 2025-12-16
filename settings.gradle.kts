pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    // The IDE needs to know the plugin IDs, but we don’t set versions here.
    // Versions are resolved from the classpath in the root build.gradle.kts.
    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
        id("org.jetbrains.kotlin.kapt")
        id("com.google.dagger.hilt.android")
        id("com.google.gms.google-services")
        id("kotlin-parcelize")
        id("dagger.hilt.android.plugin")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "KidsControlApp"
include(":app")