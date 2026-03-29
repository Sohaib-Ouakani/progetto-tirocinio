import kotlin.experimental.ExperimentalNativeApi
import native_wrapper.NativeFmiWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import native_wrapper.DLL_STATUS

class NativeFmiWrapperTest {

    @Test
    fun testCinterop() {
        val result = runCatching {
            NativeFmiWrapper(
                path = "./resources/models/BouncingBall.fmu",
                resources = "./resources/extracted"
            )
        }
        assertTrue(result.isFailure)

        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception.message!!.contains("non trovato"))
    }

    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun testFmuInfoHasVariables() {
        val wrapper = NativeFmiWrapper(
            path = "path/al/tuo.fmu",
            resources = "path/resources/"
        )
        assert(wrapper.fmuInfo.variables.isNotEmpty())
        wrapper.close()
    }
}
