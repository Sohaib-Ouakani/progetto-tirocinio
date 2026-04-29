package utility

class FmuPackager(
    private val exec: ProcessExecution,
    private val fs: FilesystemManager
) {
    fun extract(fmuPath: String, destDir: String) {
        exec.run("mkdir", "-p", destDir)
        check(exec.run("unzip", "-q", fmuPath, "-d", destDir) == 0) {
            "Extraction failed: $fmuPath"
        }
    }

    fun zip(sourceDir: String, outputFmu: String) {
        check(exec.run("cd", "\"$sourceDir\"", "&&", "zip", "-qr", "\"$outputFmu\"", ".") == 0) {
            "zip failed"
        }
    }

    fun <T> withTempDir(block: (String) -> T): T {
        val tmp = exec.runWithOutput("mktemp", "-d", "/tmp/fmu_recompile_XXXXXX")
            .also { check(it.isNotBlank()) { "Temp folder creation failed" } }
        return try {
            block(tmp)
        } finally {
            exec.run("rm", "-rf", tmp)
        }
    }
}
