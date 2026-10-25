plugins {
    alias(libs.plugins.nof1.jvm.library)
}

dependencies {
    api(projects.core.model)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}
