/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2025-2026 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

//file:noinspection HardCodedStringLiteral
plugins {
	id("com.android.application")
    id("jacoco")
}

android {
	namespace = "org.moire.opensudoku"
	compileSdk = 37

	defaultConfig {
		applicationId = "org.moire.opensudoku"
		minSdk = 26
		targetSdk = 37
		versionCode = 20260817
		versionName = "4.10.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables.useSupportLibrary = true

		ndk {
			abiFilters += listOf("arm64-v8a")
		}
	}

	buildTypes {
		getByName("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			ndk.debugSymbolLevel = "FULL"
		}
		getByName("debug") {
			versionNameSuffix = "-DBG"
			applicationIdSuffix = ".debug"
		            isTestCoverageEnabled = true
            enableAndroidTestCoverage = true
}
		create("demo") {
			initWith(getByName("release"))
			versionNameSuffix = "-DMO"
			applicationIdSuffix = ".demo"
			signingConfig = signingConfigs.getByName("debug")
		}
	}

	compileOptions {
		encoding = "UTF-8"
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	packaging {
		jniLibs {
			excludes += "META-INF/*"
		}
		resources {
			excludes += "META-INF/*"
		}
	}

	androidResources {
		noCompress += listOf("tflite", "traineddata")
	}

	lint {
	}

	buildFeatures {
		buildConfig = true
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

dependencies {
	implementation(libs.preference)
	implementation(libs.gridlayout)
	implementation(libs.material)

	// The original net.margaritov.preference.colorpicker.ColorPickerPreference does not work
	// with androidx.preference.Preference.
	// This fork does, but is not published to a repository, so use the module direct from
	// GitHub. Gradle source dependencies do not support specifying a git ref or tag, so for
	// reproducibility, use the tag and depend on jitpack.io in the parent build.gradle.
	implementation(libs.android.colorpickerpreference)

	// dependencies needed for Sudoku scanning
	implementation(libs.opencv) // camera view
	implementation(libs.tensorflow.lite)

	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.kotest.assertions.core)

	androidTestImplementation(libs.junit.ktx)
	androidTestImplementation(libs.espresso.core)
}
