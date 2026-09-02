import com.example.util.simpletimetracker.Base
import com.example.util.simpletimetracker.applyAndroidLibrary

plugins {
    alias(libs.plugins.gradleLibrary)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.ksp)
}

applyAndroidLibrary()

android {
    namespace = "${Base.namespace}.feature_views"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.cardView)
    implementation(libs.androidx.material)
}
