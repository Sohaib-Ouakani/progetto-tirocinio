import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    alias(libs.plugins.gradle.cmake)
}

//machines.customMachines.register("linux-amd64") {
//    toolchainFile.set(file("linux-x86-64.toolchain.cmake"))
//}

cmake {
    targets {
        create("fmilib") {
            cmakeLists = file("lib/CMakeLists.txt")
//            targetMachines.addAll(machines.customMachines)
            targetMachines.add(machines.host)
            cmakeArgs.add("-DXML_POOR_ENTROPY=ON")
        }
    }
}


val fmilibBuildDir = layout.buildDirectory.dir("cmake/fmilib").get().asFile
val fmilibInstallDir = layout.buildDirectory.dir("fmilib-install").get().asFile

val cmakeInstall by tasks.registering {
    dependsOn("cmakeBuild")
    outputs.dir(fmilibInstallDir)

    val execOps = project.serviceOf<ExecOperations>()

    doLast {
        val platformDirs = fmilibBuildDir.listFiles()?.filter { it.isDirectory }
            ?: error("cmake build dir non trovata in $fmilibBuildDir")

        platformDirs.forEach { platformDir ->
            val platformInstallDir = fmilibInstallDir.resolve(platformDir.name)
            execOps.exec {
                commandLine(
                    "cmake", "--install", platformDir.absolutePath,
                    "--prefix", platformInstallDir.absolutePath
                )
            }
        }
    }
}

tasks.named("cmakeBuild") {
    finalizedBy(cmakeInstall)
}

val cmakeInstallClean by tasks.registering(Delete::class) {
    delete(fmilibInstallDir)
}

tasks.named("cmakeClean") {
    finalizedBy(cmakeInstallClean)
}
