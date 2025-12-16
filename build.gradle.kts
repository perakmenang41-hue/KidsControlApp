buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // Android Gradle Plugin
        classpath("com.android.tools.build:gradle:8.2.2")
        // Kotlin plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        // Google Services (Firebase) plugin
        classpath("com.google.gms:google-services:4.4.2")
        // Hilt Gradle plugin
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51")
        // Force a recent JavaPoet version for Hilt/Kapt
        classpath("com.squareup:javapoet:1.13.0")
    }
}