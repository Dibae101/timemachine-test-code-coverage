/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import com.example.util.simpletimetracker.Base
import com.example.util.simpletimetracker.applyAndroidLibrary

plugins {
    alias(libs.plugins.gradleLibrary)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

applyAndroidLibrary()

android {
    namespace = "${Base.namespace}.feature_wear"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":wear_api"))
    implementation(libs.google.services)
    implementation(libs.google.gson)
    implementation(libs.google.dagger)
    ksp(libs.kapt.dagger)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.coroutines)
}
