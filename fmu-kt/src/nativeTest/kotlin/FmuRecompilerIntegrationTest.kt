import recompiler.FmuRecompiler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.access
import platform.posix.F_OK

class FmuRecompilerIntegrationTest {

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun recompileFgstmu() {
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
