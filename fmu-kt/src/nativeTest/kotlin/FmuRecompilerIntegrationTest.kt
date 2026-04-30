import recompiler.FmuRecompiler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.access
import platform.posix.F_OK

/**
 * Integration tests for the [FmuRecompiler] class.
 * Tests the end-to-end FMU recompilation process using real FMU files.
 */
class FmuRecompilerIntegrationTest {

    /**
     * Tests the recompilation of an FMU file specified by the TEST_FMU_PATH environment variable.
     * Verifies that the output FMU is successfully created at the expected location.
     * Skips the test if TEST_FMU_PATH is not set, for CI integration purposes.
     */
    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun recompileFmu() {
        val fmuPath = platform.posix.getenv("TEST_FMU_PATH")
            ?.toKString() ?: run {
            println("TEST_FMU_PATH not set, skipping integration test")
            return
        }

        val output = "/tmp/recompiled_test_output.fmu"
        FmuRecompiler().recompile(fmuPath, output)

        assertEquals(access(output, F_OK), 0, "Output FMU was not created")
        println("Integration test passed — output at $output")
    }
}
