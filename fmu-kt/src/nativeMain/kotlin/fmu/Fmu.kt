package fmu

import wrapper.DLLSTATUS
import wrapper.NativeFmiWrapper
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

/**
 * High-level wrapper for FMU (Functional Mock-up Unit) operations.
 * Provides a simplified interface for loading, configuring, running, and managing FMU simulations.
 * Implements [AutoCloseable] to ensure proper resource cleanup.
 *
 * @property fmuPath Path to the FMU file.
 * @property resourcesPath Path to the resources' directory.
 * @property modelsDir Directory where models are stored.
 * @property fmuInfo Information about the loaded FMU.
 */
class Fmu(val fmuPath: String, val resourcesPath: String, val modelsDir: String) : AutoCloseable {
    private val fmi: NativeFmiWrapper = NativeFmiWrapper(fmuPath, resourcesPath, modelsDir)
    val fmuInfo: FmuInfo = fmi.fmuInfo
    private var fmiClosed = false

    init {
        if(fmi.dllStatus != DLLSTATUS.OK) {
            throw IllegalArgumentException("Error when loading DLL")
        }
    }

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
