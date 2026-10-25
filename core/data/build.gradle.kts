plugins {
    alias(libs.plugins.nof1.android.library)
    alias(libs.plugins.nof1.hilt)
}

android {
    namespace = "com.nof1.core.data"
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(projects.core.database)
    implementation(projects.core.export)

    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.sqlite.bundled)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}
