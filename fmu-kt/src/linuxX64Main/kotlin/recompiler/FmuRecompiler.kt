package recompiler

/**
 * Platform-specific FMU recompilation implementation for Linux x64.
 * Currently not supported - prints a message and skips recompilation.
 */
actual class FmuRecompiler {

    /**
     * Recompiles an input FMU file and produces a new FMU at the output path.
     * On Linux x64, this operation is not supported yet and will be skipped.
     *
     * @param inputFmu The file path to the input FMU file.
     * @param outputFmu The file path where the recompiled FMU will be saved.
     */
    actual fun recompile(inputFmu: String, outputFmu: String) {
        println("Skipping recompilation on linuxX64, as it is not supported yet.")
    }

}
