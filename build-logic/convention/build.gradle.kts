import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.nof1.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // The plugins themselves come from the main build's classpath, where the root build file
    // declares them with their versions.
    compileOnly(libs.android.gradleApiPlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.nof1.android.application.get().pluginId
            implementationClass = "com.nof1.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.nof1.android.library.get().pluginId
            implementationClass = "com.nof1.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.nof1.hilt.get().pluginId
            implementationClass = "com.nof1.buildlogic.HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.nof1.jvm.library.get().pluginId
            implementationClass = "com.nof1.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
