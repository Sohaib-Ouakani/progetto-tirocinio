allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}

subprojects {
    tasks.matching {
        it.name.contains("iosSimulatorArm64Test")
    }.configureEach {
        enabled = false
    }
}
