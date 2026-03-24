allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    tasks.matching {
        it.name.contains("iosSimulatorArm64Test")
    }.configureEach {
        enabled = false
    }
}
