@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "org.findaname"

repositories {
    google()
    mavenCentral()
}

kotlin {

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":fmu-kt"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        binaries {
            executable {
                entryPoint = "main"
                linkerOpts(
                    "-L${rootProject.projectDir}/fmu-kt/libs/fmilib/lib",
                    "-lfmilib_shared",
                    "-Wl,-rpath,${rootProject.projectDir}/fmu-kt/libs/fmilib/lib"
                )
            }
        }
    }

    applyDefaultHierarchyTemplate()
    /*
     * Linux 64
     */
    linuxX64(nativeSetup)
    linuxArm64(nativeSetup)
    /*
     * Win 64
     */
    mingwX64(nativeSetup)
    /*
     * Apple OSs
     */
    macosX64(nativeSetup)
    macosArm64(nativeSetup)
    iosArm64(nativeSetup)
    iosSimulatorArm64(nativeSetup)
    watchosArm32(nativeSetup)
    watchosArm64(nativeSetup)
    watchosSimulatorArm64(nativeSetup)
    tvosArm64(nativeSetup)
    tvosSimulatorArm64(nativeSetup)

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    allWarningsAsErrors = true
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}
