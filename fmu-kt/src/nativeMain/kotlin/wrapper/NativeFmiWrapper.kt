package wrapper

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import libfmi.fmi2_fmu_kind_cs
import libfmi.fmi2_fmu_kind_me
import libfmi.fmi2_fmu_kind_me_and_cs
import libfmi.fmi2_fmu_kind_unknown
import libfmi.fmi2_import_create_dllfmu
import libfmi.fmi2_import_destroy_dllfmu
import libfmi.fmi2_import_do_step
import libfmi.fmi2_import_enter_initialization_mode
import libfmi.fmi2_import_exit_initialization_mode
import libfmi.fmi2_import_free
import libfmi.fmi2_import_free_instance
import libfmi.fmi2_import_get_author
import libfmi.fmi2_import_get_default_experiment_start
import libfmi.fmi2_import_get_default_experiment_step
import libfmi.fmi2_import_get_default_experiment_stop
import libfmi.fmi2_import_get_description
import libfmi.fmi2_import_get_fmu_kind
import libfmi.fmi2_import_get_model_name
import libfmi.fmi2_import_get_model_version
import libfmi.fmi2_import_get_real
import libfmi.fmi2_import_get_variable
import libfmi.fmi2_import_get_variable_by_name
import libfmi.fmi2_import_get_variable_list
import libfmi.fmi2_import_get_variable_list_size
import libfmi.fmi2_import_get_variable_name
import libfmi.fmi2_import_get_variable_vr
import libfmi.fmi2_import_instantiate
import libfmi.fmi2_import_parse_xml
import libfmi.fmi2_import_setup_experiment
import libfmi.fmi2_import_terminate
import libfmi.fmi2_type_t
import libfmi.fmi2_value_reference_tVar
import libfmi.fmi_import_allocate_context
import libfmi.fmi_import_context_t
import libfmi.fmi_import_free_context
import libfmi.fmi_import_get_fmi_version
import libfmi.fmi_version_2_0_enu
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult
import recompiler.FmuRecompiler

enum class DLLSTATUS {
    OK, ERROR
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
class NativeFmiWrapper(val path: String, val resources: String, val baseDir: String) : AutoCloseable {
    var context: CPointer<fmi_import_context_t>? = fmi_import_allocate_context(null)
    var fmi: CPointer<cnames.structs.fmi2_import_t>? = null
    var fmuInfo: FmuInfo
    val dllStatus: DLLSTATUS
    var experimentInstance: Int? = null
    var simulationConfig: SimulationConfig? = null

    init {
        val fmuOutputPath = "$baseDir/resources/models/result.fmu"
        var fmuFile = fmuOutputPath
        if (Platform.osFamily == OsFamily.MACOSX) {
            println("Running on  MacOS, recompiling FMU")
            val recompiler = FmuRecompiler()
            recompiler.recompile(path, fmuFile)
        } else {
            fmuFile = path
            println("Running on Windows, skipping FMU recompilation")
        }
        val version = fmi_import_get_fmi_version(
            this.context,
            fmuFile,
            resources
        )
        this.fmi = fmi2_import_parse_xml(context, resources, null)
        fmuInfo = getInfo()

        val dllResult = fmi2_import_create_dllfmu(fmi, fmi_version_2_0_enu, null)
        dllStatus = if (dllResult == 0) DLLSTATUS.OK else DLLSTATUS.ERROR
    }

    private fun getInfo(): FmuInfo {
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

    fun instantiate(experimentName: String = "default") {
        check(experimentInstance == null) { "Esperimento già istanziato" }
        experimentInstance = fmi2_import_instantiate(
            fmi,
            experimentName,
            fmi2_type_t.fmi2_cosimulation,
            null,
            0
        )
    }

    fun setupExperiment(config: SimulationConfig) {
        if (experimentInstance == null) instantiate(config.experimentName)

        this.simulationConfig = config

        fmi2_import_setup_experiment(
            fmi,
            if (config.tolerance != null) 1 else 0,
            config.tolerance ?: 0.0,
            config.startTime,
            if (config.stopTime != null) 1 else 0,
            config.stopTime ?: 0.0
        )

        fmi2_import_enter_initialization_mode(fmi)
        fmi2_import_exit_initialization_mode(fmi)
    }

    fun executeExperiment(): SimulationResult {
        checkNotNull(experimentInstance) { "Experiment instance must not be null" }
        val config = checkNotNull(simulationConfig) { "Simulation config must not be null" }

        val stopTime: Double = simulationConfig!!.stopTime
            ?: fmuInfo.defaultExperimentStop
        val step: Double = simulationConfig!!.stepSize
        var time = config.startTime

        // Determina quali variabili leggere
        val variablesToRead = config.outputVariables.ifEmpty {
            fmuInfo.variables  // tutte le variabili dell'FMU
        }

        val timestamps = mutableListOf<Double>()

        // Una lista per ogni variabile
        val results = variablesToRead.associateWith { mutableListOf<Double>() }

        try {
            memScoped {
                // Risolvi i value reference per ogni variabile
                val vrMap = variablesToRead.associateWith { varName ->
                    val variable = fmi2_import_get_variable_by_name(fmi, varName)
                        ?: error("Variabile '$varName' non trovata nell'FMU")
                    fmi2_import_get_variable_vr(variable)
                }

                val n = variablesToRead.size
                val vrArray = allocArray<fmi2_value_reference_tVar>(n)
                val valueArray = allocArray<DoubleVar>(n)

                variablesToRead.forEachIndexed { i, varName ->
                    vrArray[i] = vrMap[varName]!!
                }


                println("---- Simulation Start ----")

                while (time < stopTime) {

                    fmi2_import_do_step(
                        fmi,
                        time,
                        step,
                        1
                    )

                    fmi2_import_get_real(
                        fmi,
                        vrArray,
                        n.toULong(),
                        valueArray
                    )

                    timestamps.add(time)
                    variablesToRead.forEachIndexed { i, varName ->
                        results[varName]!!.add(valueArray[i])
                    }

                    time += step
                }

                println("---- Simulation End ----")

                fmi2_import_terminate(fmi)
                fmi2_import_free_instance(fmi)

                experimentInstance = null
                simulationConfig = null
            }
            return SimulationResult(
                timestamps = timestamps,
                variables = results,
                config = config
            )
        } finally {
            runCatching { fmi2_import_terminate(fmi) }
            runCatching { fmi2_import_free_instance(fmi) }
            experimentInstance = null
            simulationConfig = null
        }
    }

    fun destroyFmi() {
        fmi2_import_destroy_dllfmu(fmi)
        fmi2_import_free(fmi)
        fmi_import_free_context(context)
    }

    override fun close() {
        runCatching {
            if (experimentInstance != null) fmi2_import_terminate(fmi)
        }
        runCatching { fmi2_import_free_instance(fmi) }
        runCatching { fmi2_import_destroy_dllfmu(fmi) }
        runCatching { fmi2_import_free(fmi) }
        runCatching { fmi_import_free_context(context) }
    }
}
