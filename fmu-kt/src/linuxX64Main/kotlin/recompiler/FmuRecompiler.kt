package recompiler

actual class FmuRecompiler {

    actual fun recompile(inputFmu: String, outputFmu: String) {
        println("Skipping recompilation on linuxX64, as it is not supported yet.")
    }

}
