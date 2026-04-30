package recompiler

/**
 * Platform-specific FMU recompilation functionality.
 * Provides methods to recompile FMU files for the current platform.
 * Actual implementation is provided by platform-specific source sets.
 */
expect class FmuRecompiler() {
    /**
     * Recompiles an input FMU file and produces a new FMU at the output path.
     * The recompilation process involves extracting the FMU, compiling source files,
     * linking into platform-specific binaries, and repackaging.
     *
     * @param inputFmu The file path to the input FMU file.
     * @param outputFmu The file path where the recompiled FMU will be saved.
     */
    fun recompile(inputFmu: String, outputFmu: String)
}
