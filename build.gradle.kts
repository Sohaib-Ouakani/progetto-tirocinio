allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.gradle.cmake) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    id("com.dorongold.task-tree") version "4.0.1"
    `lifecycle-base`
}

subprojects {
    // TODO: is this needed?
    tasks.matching {
        it.name.contains("iosSimulatorArm64Test")
    }.configureEach {
        enabled = false
    }
}
