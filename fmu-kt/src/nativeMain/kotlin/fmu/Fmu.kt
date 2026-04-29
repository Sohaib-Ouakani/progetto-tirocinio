package fmu

import wrapper.DLLSTATUS
import wrapper.NativeFmiWrapper
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

class Fmu(val fmuPath: String, val resourcesPath: String, val baseDir: String) : AutoCloseable {
    private val fmi: NativeFmiWrapper = NativeFmiWrapper(fmuPath, resourcesPath, baseDir)
    val fmuInfo: FmuInfo = fmi.fmuInfo
    private var fmiClosed = false

    init {
        if(fmi.dllStatus != DLLSTATUS.OK) {
            throw IllegalArgumentException("Error when loading DLL")
        }
    }

    fun initializeExperiment(config: SimulationConfig) {
        checkFmiClosed()
       fmi.setupExperiment(config)
    }

    fun startExperiment(): SimulationResult {
        checkFmiClosed()
        return fmi.executeExperiment()
    }

    fun terminateFmu() {
        checkFmiClosed()
        fmi.destroyFmi()
    }

    private fun checkFmiClosed(){
        check(!fmiClosed) { "FMU già chiuso" }
    }

    override fun close() {
        if (!fmiClosed) {
            fmi.close()
            fmiClosed = true
        }
    }
}
