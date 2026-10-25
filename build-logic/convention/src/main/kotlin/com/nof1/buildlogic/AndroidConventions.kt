package com.nof1.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion

internal object AndroidSdk {
    const val COMPILE = 37
    const val MIN = 26
    const val TARGET = 37
}

/**
 * The emulator that instrumented tests run on, created and started by Gradle. Run a module's tests
 * on it with `./gradlew :module:pixel6Api30DebugAndroidTest`.
 */
internal const val TEST_DEVICE_NAME = "pixel6Api30"

/** Settings shared by the app and library modules. */
internal fun configureAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = AndroidSdk.COMPILE
        defaultConfig.apply {
            minSdk = AndroidSdk.MIN
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        lint.configureLint()
        testOptions.managedDevices.localDevices.create(TEST_DEVICE_NAME) {
            device = "Pixel 6"
            apiLevel = 30
            // Automated Test Device images leave out apps and services that tests don't need.
            systemImageSource = "aosp-atd"
            // Runs with hardware acceleration on x86-64 machines such as CI's. AGP 10 changes the
            // default to arm64-v8a, which this image can't run.
            testedAbi = "x86"
        }
    }
}

/** Lint settings for every module, including the pure-Kotlin ones. */
internal fun Lint.configureLint() {
    warningsAsErrors = true
    // Version-freshness checks would fail the build whenever a library ships an update.
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}
