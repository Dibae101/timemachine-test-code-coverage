plugins {
    id(libs.plugins.javaLibrary.get().pluginId)
    alias(libs.plugins.kotlinLibrary)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    api(libs.javax)
    api(libs.coroutines)
    api(libs.timber)
    api(libs.kotlin)
}
