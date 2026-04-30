package recompiler

actual class FmuRecompiler {
    fun recompile(inputFmu: String, outputFmu: String) {
        println("Skipping recompilation on mingwX64, as it is not supported yet.")
    }
}
