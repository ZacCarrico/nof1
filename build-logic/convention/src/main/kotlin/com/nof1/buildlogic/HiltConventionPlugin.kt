package com.nof1.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Hilt with KSP, for the app and Android library modules. It waits for the Android plugin. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.base") {
                pluginManager.apply("com.google.devtools.ksp")
                pluginManager.apply("com.google.dagger.hilt.android")
                dependencies {
                    add("implementation", libs.findLibrary("hilt.android").get())
                    add("ksp", libs.findLibrary("hilt.compiler").get())
                }
            }
        }
    }
}
