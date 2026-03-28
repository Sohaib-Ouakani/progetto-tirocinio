import kotlin.experimental.ExperimentalNativeApi
import native_wrapper.NativeFmiWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import native_wrapper.DLL_STATUS

class NativeFmiWrapperTest {

    @Test
    fun testFmuLoadsCorrectly() {
        val wrapper = NativeFmiWrapper(
            path = "./resources/models/BouncingBall.fmu",
            resources = "./resources/extracted"
        )
        assertNotNull(wrapper.fmuInfo)
        assertEquals(DLL_STATUS.OK, wrapper.dllStatus)
        wrapper.close()
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
