package configuration

import io.ktor.server.application.*
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.routing.*
import io.ktor.server.response.*
import model.fmu.Fmu

fun Application.configureRouting(baseDir: String) {
    val fmuPath = "$baseDir/resources/models/BouncingBall.fmu"
    val extractedPath = "$baseDir/resources/extracted"

    var fmu: Fmu? = null

    install(createApplicationPlugin("FmuCleanup") {
        on(MonitoringEvent(ApplicationStopped)) {
            fmu?.close()
            fmu = null
        }
    })

    routing {
        get("/health") {
            call.respondText("OK")
        }

        get("/") {
            call.respondText("Welcome to the home of the api")
        }

        route("/fmi") {
            get("/init") {
                try {
                fmu = Fmu(fmuPath, extractedPath)
                } catch (e: Exception) {
                    call.respondText("Error initializing FMU: ${e.message}")
                    return@get
                }
                //fmu = Fmu(FMU_PATH, resources)
                call.respondText("to view info about the fmu type /fmi/info")
            }

            get("/info") {
                val currentFmu = fmu ?: return@get call.respondText("please initiate...")
                try {
                    val result = currentFmu.fmuInfo
                    call.respond(result)
                } finally {
                    currentFmu.close()
                    fmu = null
                }
            }
        }
    }
}
