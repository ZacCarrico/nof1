plugins {
    alias(libs.plugins.nof1.android.library)
    alias(libs.plugins.nof1.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.nof1.core.database"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

room3 {
    // Every schema version is exported here and committed, for migration tests.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}
