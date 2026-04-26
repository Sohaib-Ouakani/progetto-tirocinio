import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.cinterop.ExperimentalForeignApi
import libfmi.fmi_version_2_0_enu

class NativeFmiWrapperTest {

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testCinterop() {
//        //errore creato appositamente per fallire,
//        // fmu-kt non può raggiungere la cartelle delle FMU perche stanno nel mdoulo backend
//        val result = runCatching {
//            NativeFmiWrapper(
//                path = "./resources/models/BouncingBall.fmu",
//                resources = "./resources/extracted"
//            )
//        }
//        assertTrue(result.isFailure)
//
//        val exception = result.exceptionOrNull()
//        assertTrue(exception is IllegalStateException)
//        assertTrue(exception.message!!.contains("non trovato"))

        assertNotNull(fmi_version_2_0_enu)
        println("FMILib version enum: $fmi_version_2_0_enu")
    }

//    @OptIn(ExperimentalNativeApi::class)
//    @Test
//    fun testFmuInfoHasVariables() {
//        val wrapper = NativeFmiWrapper(
//            path = "path/al/tuo.fmu",
//            resources = "path/resources/"
//        )
//        assert(wrapper.fmuInfo.variables.isNotEmpty())
//        wrapper.close()
//    }
}
