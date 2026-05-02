package wrapper.fmuData.infoManager

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import libfmi.fmi2_fmu_kind_cs
import libfmi.fmi2_fmu_kind_me
import libfmi.fmi2_fmu_kind_me_and_cs
import libfmi.fmi2_fmu_kind_unknown
import libfmi.fmi2_import_get_author
import libfmi.fmi2_import_get_default_experiment_start
import libfmi.fmi2_import_get_default_experiment_step
import libfmi.fmi2_import_get_default_experiment_stop
import libfmi.fmi2_import_get_description
import libfmi.fmi2_import_get_fmu_kind
import libfmi.fmi2_import_get_model_name
import libfmi.fmi2_import_get_model_version
import libfmi.fmi2_import_get_variable
import libfmi.fmi2_import_get_variable_list
import libfmi.fmi2_import_get_variable_list_size
import libfmi.fmi2_import_get_variable_name
import wrapper.fmuData.info.FmuInfo
import wrapper.fmuLifecycle.FmuLifecycleManager

@OptIn(ExperimentalForeignApi::class)
class InfoManagerFmu(val lifecycle: FmuLifecycleManager) {
    fun extractFmuInfo(): FmuInfo {
        val fmi = requireNotNull(lifecycle.fmiStruct) { "FMU not started" }

        val kind = when(fmi2_import_get_fmu_kind(fmi)) {
            fmi2_fmu_kind_me -> "Model Exchange"
            fmi2_fmu_kind_cs -> "Co-Simulation"
            fmi2_fmu_kind_me_and_cs -> "Model Exchange and Co-Simulation"
            fmi2_fmu_kind_unknown -> "Unknown"
            else -> ""
        }
        val varList = fmi2_import_get_variable_list(fmi, 0)
        val varlistSize = fmi2_import_get_variable_list_size(varList)
        val varibles = mutableListOf<String>()

        for (i in 0 until varlistSize.toInt()) {
            val variable = fmi2_import_get_variable(varList, i.toULong())
            val name = fmi2_import_get_variable_name(variable)?.toKString().orEmpty()
            varibles.add(name)
        }

        return FmuInfo(
            fmi2_import_get_model_name(fmi)?.toKString(),
            fmi2_import_get_description(fmi)?.toKString(),
            fmi2_import_get_author(fmi)?.toKString(),

            fmi2_import_get_model_version(fmi)?.toKString(),
            fmi2_import_get_default_experiment_start(fmi),
            fmi2_import_get_default_experiment_stop(fmi),
            fmi2_import_get_default_experiment_step(fmi),
            kind,
            varibles
        )
    }
}
