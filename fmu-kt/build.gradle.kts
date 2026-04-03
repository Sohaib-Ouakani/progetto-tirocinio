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

val cLibrary by configurations.creating

dependencies {
    cLibrary(project(":fmilib"))
}

kotlin {
    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        compilations["main"].cinterops {
            val libfmi by creating {
                headers = files("libs/fmilib/include/fmilib.h")
                compilerOpts("-I/Users/sohaibouakani/Desktop/tirocinio/progetto-tirocinio/template-for-kotlin-multiplatform-projects/fmu-kt/libs/fmilib/include -DFMILIB_EXPORT=")
//                definitionFile = file("src/nativeInterop/cinterop/libfmi.def")
            }
        }
        binaries {
            all {
                val fmilibBuildDir = project(":fmilib").layout.buildDirectory.dir("cmake/fmilib/").get().asFile.absoluteFile
                linkerOpts(
                    "-L${fmilibBuildDir.resolve("linux-amd64").absolutePath}",
                    "-lfmilib_shared",
                    "-Wl,-rpath,${fmilibBuildDir.resolve("linux-amd64").absolutePath}"
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

    applyDefaultHierarchyTemplate()
    linuxX64(nativeSetup)
//    linuxArm64(nativeSetup)
    mingwX64(nativeSetup)
//    macosX64(nativeSetup)
    macosArm64(nativeSetup)
//    iosArm64(nativeSetup)
//    iosSimulatorArm64(nativeSetup)
//    watchosArm32(nativeSetup)
//    watchosArm64(nativeSetup)
//    watchosSimulatorArm64(nativeSetup)
//    tvosArm64(nativeSetup)
//    tvosSimulatorArm64(nativeSetup)
}
