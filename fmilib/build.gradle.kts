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
        }
    }
}
