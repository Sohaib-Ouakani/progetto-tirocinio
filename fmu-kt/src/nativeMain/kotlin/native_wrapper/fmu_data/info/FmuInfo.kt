package native_wrapper.fmu_data.info

import kotlinx.serialization.Serializable

@Serializable
data class FmuInfo(
    val modelName: String?,
    val description: String?,
    val author: String?,
    val fmuVersion: String?,
    val defaultExperimentStart: Double = 0.0,
    val defaultExperimentStop: Double = 1.0,
    val defaultExperimentStep: Double = 0.01,
    val fmuKind: String?,
    val variables: List<String>,
)