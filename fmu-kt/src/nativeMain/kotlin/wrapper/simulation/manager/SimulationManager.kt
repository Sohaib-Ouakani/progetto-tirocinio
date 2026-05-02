package wrapper.simulation.manager

import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import libfmi.fmi2_import_do_step
import libfmi.fmi2_import_enter_initialization_mode
import libfmi.fmi2_import_exit_initialization_mode
import libfmi.fmi2_import_free_instance
import libfmi.fmi2_import_get_real
import libfmi.fmi2_import_get_variable_by_name
import libfmi.fmi2_import_get_variable_vr
import libfmi.fmi2_import_instantiate
import libfmi.fmi2_import_setup_experiment
import libfmi.fmi2_import_terminate
import libfmi.fmi2_type_t
import libfmi.fmi2_value_reference_tVar
import wrapper.fmuData.infoManager.InfoManagerFmu
import wrapper.fmuLifecycle.FmuLifecycleManager
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

@OptIn(ExperimentalForeignApi::class)
class SimulationManager(
    val lifecycleManager: FmuLifecycleManager,
    val infoManagerFmu: InfoManagerFmu
) {
    private var isSimulationSetup: Boolean = false
    var simulationConfig: SimulationConfig? = null
    var simulationResult: SimulationResult? = null

    fun setUpExperiment(config: SimulationConfig, experimentName: String = "default") {
        if (!isSimulationSetup) instantiate(config.experimentName)

        simulationConfig = config

        fmi2_import_setup_experiment(
            lifecycleManager.fmiStruct,
            if (config.tolerance != null) 1 else 0,
            config.tolerance ?: 0.0,
            config.startTime,
            if (config.stopTime != null) 1 else 0,
            config.stopTime ?: 0.0
        )

        fmi2_import_enter_initialization_mode(lifecycleManager.fmiStruct)
        fmi2_import_exit_initialization_mode(lifecycleManager.fmiStruct)
    }

    fun executeSimulation(): SimulationResult {
        if (!isSimulationSetup) {
            throw IllegalStateException("Simulation not set up.")
        }
        if (simulationConfig == null) {
            throw IllegalStateException("Simulation config is null.")
        }

        val config = simulationConfig!!
        val fmuInfo = infoManagerFmu.extractFmuInfo()

        val stopTime: Double = config.stopTime
            ?: fmuInfo.defaultExperimentStop
        val step: Double = config.stepSize
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
                    val variable = fmi2_import_get_variable_by_name(lifecycleManager.fmiStruct, varName)
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
                        lifecycleManager.fmiStruct,
                        time,
                        step,
                        1
                    )

                    fmi2_import_get_real(
                        lifecycleManager.fmiStruct,
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

                fmi2_import_terminate(lifecycleManager.fmiStruct)
                fmi2_import_free_instance(lifecycleManager.fmiStruct)

                isSimulationSetup = false
                simulationConfig = null
            }
            val results = SimulationResult(
                timestamps = timestamps,
                variables = results,
                config = config
            )
            simulationResult = results

            return results
        } finally {
            try {
                fmi2_import_terminate(lifecycleManager.fmiStruct)
                fmi2_import_free_instance(lifecycleManager.fmiStruct)
            } catch (e: Exception) {
                throw IllegalStateException("Error during simulation cleanup: ${e.message}", e)
            }
            isSimulationSetup = false
            simulationConfig = null
        }
    }

    private fun instantiate(experimentName: String = "default") {
        if(isSimulationSetup) {
            throw IllegalStateException("Already instanced simulation")
        }
        val status = fmi2_import_instantiate(
            lifecycleManager.fmiStruct,
            experimentName,
            fmi2_type_t.fmi2_cosimulation,
            null,
            0
        )

        if (status != 0) {
            throw IllegalStateException("Failed to instantiate the simulation")
        }
    }
}
