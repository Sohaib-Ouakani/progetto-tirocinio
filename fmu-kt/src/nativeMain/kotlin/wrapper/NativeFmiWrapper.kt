package wrapper

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import preprocessor.FmuPreprocessor
import wrapper.fmuData.info.FmuInfo
import wrapper.fmuData.infoManager.InfoManagerFmu
import wrapper.fmuLifecycle.FmuLifecycleManager
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.manager.SimulationManager
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

    val fmuLifecycle: FmuLifecycleManager = FmuLifecycleManager(
        preprocessor.prepare(path, modelsDir),
        resources
    )
    val infoFmu: InfoManagerFmu = InfoManagerFmu(fmuLifecycle)
    val simulationManager: SimulationManager = SimulationManager(fmuLifecycle, infoFmu)

    init {
        fmuLifecycle.start()
    }

    fun getInfo(): FmuInfo {
        return infoFmu.extractFmuInfo()
    }

    /**
     * Sets up the experiment with the given simulation configuration.
     * Configures the FMU for simulation including tolerance, start/stop times, and enters initialization mode.
     * If no experiment instance exists, one will be created automatically.
     *
     * @param config The simulation configuration containing parameters like start time, stop time, tolerance, etc.
     */
    fun setupExperiment(config: SimulationConfig) {
        simulationManager.setUpExperiment(config)
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
        return simulationManager.executeSimulation()
    }

    /**
     * Closes the FMU wrapper and releases all associated resources.
     * This method is called automatically when using try-with-resources or when the object is garbage collected.
     * Safely handles cleanup even if some operations fail, using runCatching to prevent exceptions.
     * After calling this method, the wrapper cannot be used for further operations.
     */
    override fun close() {
        fmuLifecycle.close()
    }
}
