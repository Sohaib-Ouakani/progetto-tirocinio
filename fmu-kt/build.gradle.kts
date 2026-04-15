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

//per trovare il percorso dove sta gcc
val libGccPath: String by lazy {
    try {
        val process = Runtime.getRuntime().exec("gcc -print-file-name=crtbegin.o")
        val output = process.inputStream.bufferedReader().readText().trim()
        if (output.isNotEmpty() && output != "crtbegin.o") {
            File(output).parent // Restituisce la directory contenente crtbegin.o
        } else {
            // Fallback nel caso in cui il comando non restituisca un percorso valido
            "/usr/lib/gcc/x86_64-linux-gnu/11"
        }
    } catch (e: Exception) {
        // Fallback in caso di errore nell'esecuzione del comando
        "/usr/lib/gcc/x86_64-linux-gnu/11"
    }
}

// Funzione che restituisce sia lib (per le .dll.a) che bin (per le .dll)
fun getNativeBinDirs(targetName: String): List<File> {
    val platformDir = fmilibInstallDir.resolve(platformDirName[targetName] ?: error("Unknown target $targetName"))
    return listOf(platformDir.resolve("lib"), platformDir.resolve("bin"))
}

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
                compilerOpts("-I${includeDir}")
            }
        }
        binaries {
            all {
                if (targetName == "linuxX64") {
                    linkerOpts(
                        "-L${libDir}",
                        "-lfmilib_shared",
                        "-Wl,-rpath,${libDir}",
                        "-L/usr/lib/x86_64-linux-gnu"        // libdl, libm, libc, ecc.
                    )
                } else {
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

    // Override linker per Linux — separato da nativeSetup
    linuxX64 {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add(
                        "-Xoverride-konan-properties=linker.linux_x64=/usr/bin/ld"
                    )
                }
            }
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

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    if (name == "mingwX64Test") {
        val binDirs = getNativeBinDirs("mingwX64")
        doFirst {
            val currentPath = environment["PATH"] ?: ""
            val newPath = binDirs.joinToString(File.pathSeparator) { it.absolutePath } +
                File.pathSeparator + currentPath
            environment("PATH", newPath)
            logger.lifecycle("PATH per mingwX64Test: $newPath")
        }
    }
}
