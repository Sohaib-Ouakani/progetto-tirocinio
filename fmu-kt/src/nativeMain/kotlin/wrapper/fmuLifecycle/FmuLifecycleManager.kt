package wrapper.fmuLifecycle

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import libfmi.fmi2_fmu_kind_me
import libfmi.fmi2_import_create_dllfmu
import libfmi.fmi2_import_destroy_dllfmu
import libfmi.fmi2_import_free
import libfmi.fmi2_import_free_instance
import libfmi.fmi2_import_get_fmu_kind
import libfmi.fmi2_import_parse_xml
import libfmi.fmi2_import_terminate
import libfmi.fmi_import_allocate_context
import libfmi.fmi_import_context_t
import libfmi.fmi_import_free_context
import libfmi.fmi_import_get_fmi_version
import libfmi.fmi_version_2_0_enu
import logger.Logger
import wrapper.DLLSTATUS

@OptIn(ExperimentalForeignApi::class)
class FmuLifecycleManager(val fmuFile: String, val unpackDir: String) {
    val context: CPointer<fmi_import_context_t>? = fmi_import_allocate_context(null)
    var fmiStruct: CPointer<cnames.structs.fmi2_import_t>? = null
        private set
    var canSimulate: Boolean = false

    init {
        fmi_import_get_fmi_version(this.context, fmuFile, unpackDir)
    }

    fun start() {
        fmiStruct = fmi2_import_parse_xml(context, unpackDir, null)
        requireNotNull(fmiStruct) {
            "Failed to parse XML. The FMU may be corrupt or unsupported."
        }

        val fmuKind = fmi2_import_get_fmu_kind(fmiStruct)

        if (fmuKind == fmi2_fmu_kind_me) {
            Logger.i("FMU is of type Model Exchange. Simulation not supported.")
        } else {
            canSimulate = true
        }

        val dllResult = fmi2_import_create_dllfmu(fmiStruct, fmi_version_2_0_enu, null)
        val dllStatus = if (dllResult == 0) DLLSTATUS.OK else DLLSTATUS.ERROR

        if (dllStatus == DLLSTATUS.ERROR) {
            throw IllegalStateException("Error when loading FMU binary")
        }
    }

    fun close() {
        try {
            fmi2_import_terminate(fmiStruct)
            fmi2_import_free_instance(fmiStruct)
            fmi2_import_destroy_dllfmu(fmiStruct)
            fmi2_import_free(fmiStruct)
            fmi_import_free_context(context)
        } catch (e: Exception) {
            throw IllegalStateException("Error during FMU closing: ${e.message}", e)
        }
    }
}
