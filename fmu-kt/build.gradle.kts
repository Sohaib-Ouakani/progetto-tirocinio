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
val platformDirName = mapOf(
    "macosArm64"  to "mac-aarch64",
    "linuxX64"    to "linux-amd64",
    "mingwX64"    to "windows-amd64"
)

val fmilibInstallDir = project(":fmilib").layout.buildDirectory.dir("fmilib-install").get().asFile

kotlin {
    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        val platformDir = fmilibInstallDir
            .resolve(platformDirName[targetName] ?: error("Platform $targetName sconosciuta"))
        val includeDir = platformDir.resolve("include")
        val libDir = platformDir.resolve("lib")

        compilations["main"].cinterops {
            val libfmi by creating {
                headers = files(includeDir.resolve("fmilib.h"))
                compilerOpts("-I${includeDir}", "-DFMILIB_EXPORT=")
            }
        }
        binaries {
            all {
                if (targetName == "mingwX64") {
                    // Windows: link statico, nessuna DLL necessaria a runtime
                    linkerOpts(
                        "-L${libDir}",
                        "-lfmilib"   // .a statica, non _shared
                    )
                } else {
                    // Linux/macOS: link dinamico con rpath
                    linkerOpts(
                        "-L${libDir}",
                        "-lfmilib_shared",
                        "-Wl,-rpath,${libDir}"
                    )
                }
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
