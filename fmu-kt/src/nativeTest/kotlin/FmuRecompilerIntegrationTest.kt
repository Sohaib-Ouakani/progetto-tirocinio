import recompiler.FmuRecompiler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.access
import platform.posix.F_OK

class FmuRecompilerIntegrationTest {

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun `recompile real fmu end to end`() {
        val fmuPath = "/Users/sohaibouakani/Desktop/tirocinio/progetto-tirocinio/template-for-kotlin-multiplatform-projects/BouncingBall.fmu"

        val output = "/tmp/recompiled_test_output.fmu"
        FmuRecompiler().recompile(fmuPath, output)

        assertEquals(access(output, F_OK), 0, "Output FMU was not created")
        println("Integration test passed — output at $output")
    }
}
