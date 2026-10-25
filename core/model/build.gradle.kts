plugins {
    alias(libs.plugins.nof1.jvm.library)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}
