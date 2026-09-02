import com.example.util.simpletimetracker.Base
import com.example.util.simpletimetracker.applyAndroidLibrary

plugins {
    alias(libs.plugins.gradleLibrary)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinParcelize)
}

applyAndroidLibrary()

android {
    namespace = "${Base.namespace}.feature_icon_selection.api"
}

dependencies {
    implementation(project(":core"))
}
