import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "org.findaname"

repositories {
    google()
    mavenCentral()
}

kotlin {
    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        compilations["main"].cinterops {
            val libfmi by creating {
                definitionFile = file("src/nativeInterop/cinterop/libfmi.def")
            }
        }
        binaries {
            all {
                linkerOpts(
                    "-L${projectDir}/libs/fmilib/lib",
                    "-lfmilib_shared",
                    "-Wl,-rpath,${projectDir}/libs/fmilib/lib"
                )
            }
            staticLib()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
            }
        }
    }

   // applyDefaultHierarchyTemplate()
    linuxX64(nativeSetup)
    linuxArm64(nativeSetup)
    mingwX64(nativeSetup)
    macosX64(nativeSetup)
    macosArm64(nativeSetup)
//    iosArm64(nativeSetup)
//    iosSimulatorArm64(nativeSetup)
//    watchosArm32(nativeSetup)
//    watchosArm64(nativeSetup)
//    watchosSimulatorArm64(nativeSetup)
//    tvosArm64(nativeSetup)
//    tvosSimulatorArm64(nativeSetup)
}
