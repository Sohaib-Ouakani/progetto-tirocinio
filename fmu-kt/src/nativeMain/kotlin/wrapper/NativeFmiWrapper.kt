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
import libfmi.fmi2_import_get_last_error
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
import preprocessor.FmuPreprocessor
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

/**
 * Status of DLL loading operations.
 */
enum class DLLSTATUS {
    /**
     * Status for successful loading operation.
     */
    OK,
    /**
     * Status for failed loading operation.
     */
    ERROR
}

/**
 * Low-level wrapper for native FMI (Functional Mock-up Interface) operations.
 * Provides direct access to FMILib functions for loading, configuring, and running FMU simulations.
 * Handles platform-specific recompilation on macOS and manages FMU lifecycle.
 *
 * @property path Path to the FMU file.
 * @property resources Path to the resource directory.
 * @property modelsDir Directory where models are stored.
 * @property context FMILib context pointer.
 * @property fmi FMILib import pointer.
 * @property fmuInfo Information about the loaded FMU.
 * @property dllStatus Status of DLL loading.
 * @property experimentInstance Current experiment instance ID, null if not instantiated.
 * @property simulationConfig Current simulation configuration, null if not set.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
class NativeFmiWrapper(
    val path: String,
    val resources: String,
    val modelsDir: String,
    preprocessor: FmuPreprocessor
) : AutoCloseable {

    var context: CPointer<fmi_import_context_t>? = fmi_import_allocate_context(null)
    var fmi: CPointer<cnames.structs.fmi2_import_t>? = null
    var fmuInfo: FmuInfo
    val dllStatus: DLLSTATUS
    var experimentInstance: Int? = null
    var simulationConfig: SimulationConfig? = null

    init {
        var fmuFile = preprocessor.prepare(path, modelsDir)

        val version = fmi_import_get_fmi_version(
            this.context,
            fmuFile,
            resources
        )
        this.fmi = fmi2_import_parse_xml(context, resources, null)
        requireNotNull(this.fmi) {
            "Failed to parse XML. The FMU may be corrupt or unsupported."
        }
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

    private fun instantiate(experimentName: String = "default") {
        check(experimentInstance == null) { "Esperimento già istanziato" }
        experimentInstance = fmi2_import_instantiate(
            fmi,
            experimentName,
            fmi2_type_t.fmi2_cosimulation,
            null,
            0
        )
    }

    /**
     * Sets up the experiment with the given simulation configuration.
     * Configures the FMU for simulation including tolerance, start/stop times, and enters initialization mode.
     * If no experiment instance exists, one will be created automatically.
     *
     * @param config The simulation configuration containing parameters like start time, stop time, tolerance, etc.
     */
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

    /**
     * Executes the simulation experiment and returns the results.
     * Runs the simulation from start time to stop time, recording variable values at each time step.
     * Uses the variables specified in the simulation config, or all variables if none specified.
     *
     * @return A [SimulationResult] containing timestamps and variable values from the simulation.
     * @throws IllegalStateException if no experiment instance or simulation config is set.
     */
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

    /**
     * Closes the FMU wrapper and releases all associated resources.
     * This method is called automatically when using try-with-resources or when the object is garbage collected.
     * Safely handles cleanup even if some operations fail, using runCatching to prevent exceptions.
     * After calling this method, the wrapper cannot be used for further operations.
     */
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
