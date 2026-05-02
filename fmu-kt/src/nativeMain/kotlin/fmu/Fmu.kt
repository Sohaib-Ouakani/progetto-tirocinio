package fmu

import preprocessor.factory.createPreprocessor
import wrapper.NativeFmiWrapper
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

/**
 * High-level wrapper for FMU (Functional Mock-up Unit) operations.
 * Provides a simplified interface for loading, configuring, running, and managing FMU simulations.
 * Implements [AutoCloseable] to ensure proper resource cleanup.
 *
 * This class acts as the main entry point for FMU interactions, hiding complexity of the low-level
 * [NativeFmiWrapper] and providing a clean API for users.
 *
 * @property fmuPath Path to the FMU file to be loaded.
 * @property resourcesPath Path to the resources directory containing extracted FMU contents.
 * @property modelsDir Directory where compiled models are stored.
 * @property fmuInfo FMU metadata including model name, description, and available variables.
 * @throws IllegalArgumentException if the FMU cannot be loaded or is corrupted.
 */
class Fmu(val fmuPath: String, val resourcesPath: String, val modelsDir: String) : AutoCloseable {
    private val fmi: NativeFmiWrapper = NativeFmiWrapper(
        fmuPath,
        resourcesPath,
        modelsDir,
        createPreprocessor()
    )
    val fmuInfo: FmuInfo = fmi.getInfo()
    private var fmiClosed = false

    /**
     * Initializes the FMU experiment with the given configuration.
     * Sets up the experiment parameters but does not start the simulation.
     *
     * @param config The simulation configuration to use.
     * @throws IllegalStateException if the FMU has already been closed.
     */
    fun initializeExperiment(config: SimulationConfig) {
        checkFmiClosed()
       fmi.setupExperiment(config)
    }

    /**
     * Starts and executes the simulation experiment.
     * Runs the simulation from start time to stop time using the configured parameters.
     *
     * @return The [SimulationResult] containing timestamps and variable values from the simulation.
     * @throws IllegalStateException if the FMU has already been closed.
     */
    fun startExperiment(): SimulationResult {
        checkFmiClosed()
        return fmi.executeExperiment()
    }

    private fun checkFmiClosed(){
        check(!fmiClosed) { "FMU già chiuso" }
    }

    /**
     * Closes the FMU and releases all associated resources.
     * This method is called automatically when using try-with-resources or when the object is garbage collected.
     * Safe to call multiple times.
     */
    override fun close() {
        if (!fmiClosed) {
            fmi.close()
            fmiClosed = true
        }
    }
}
