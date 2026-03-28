package native_wrapper.simulation.config

data class SimulationConfig(
    val startTime: Double = 0.0,
    val stopTime: Double? = null,
    val stepSize: Double = 0.01,
    val tolerance: Double? = null,
    val experimentName: String = "Default",
    val outputVariables: List<String> = emptyList()
)