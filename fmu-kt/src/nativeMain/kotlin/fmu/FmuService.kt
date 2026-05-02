package fmu

import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

interface FmuService: AutoCloseable {
    fun load(fmuPath: String, resourcesPath: String, modelsDir: String): Boolean
    fun getInfo(): FmuInfo
    fun simulate(config: SimulationConfig): SimulationResult
}
