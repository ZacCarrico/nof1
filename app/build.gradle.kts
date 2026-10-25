plugins {
    alias(libs.plugins.nof1.android.application)
    alias(libs.plugins.nof1.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.nof1"

    defaultConfig {
        applicationId = "com.nof1"
        versionCode = 1
        versionName = "0.1.0"
        // Runs instrumented tests in Hilt's test application.
        testInstrumentationRunner = "com.nof1.HiltTestRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(projects.core.database)
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)
}
