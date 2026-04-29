package wrapper.simulation.results

import wrapper.simulation.config.SimulationConfig

data class SimulationResult (
    val timestamps: List<Double>,
    val variables: Map<String, List<Double>>,
    val config: SimulationConfig
)
