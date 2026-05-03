package configuration

import io.ktor.server.application.*
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.routing.*
import io.ktor.server.response.*
import fmu.FmuService
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import logger.BackendLogger
import resources.manager.ResourceManagerService

fun Application.configureRouting(resourceManger: ResourceManagerService, fmuService: FmuService) {

    install(createApplicationPlugin("FmuCleanup") {
        on(MonitoringEvent(ApplicationStopped)) {
            fmuService.close()
        }
    })

    routing {
        get("/health") { call.respondText("OK") }
        get("/") { call.respondText("Welcome to the home of the api") }

        route("/fmi") {
            install(ContentNegotiation) { json() }
            route("/init") {
                get {
                    try {
                        fmuService.load(resourceManger.fmuPaths())
                    } catch (e: Exception) {
                        BackendLogger.e("Error initializing FMU: ${e.message}")

                        call.respondText(
                            "Error initializing FMU: ${e.message}",
                            status = HttpStatusCode.InternalServerError
                        )
                        return@get
                    }

                    call.respondText("to view info about the fmu type /fmi/info")
                }
            }

            route("/info") {
                get {
                    try {
                        val result = fmuService.getInfo()
                        call.respond(result)
                    } catch (e: Exception) {
                        call.respondText(
                            "Error while getting fmu info: ${e.message}",
                            status = HttpStatusCode.InternalServerError
                        )
                        return@get
                    }
                }
            }

            post("/upload") {
                val fileName = call.request.header(HttpHeaders.ContentDisposition)
                    ?.let { ContentDisposition.parse(it).parameter(ContentDisposition.Parameters.FileName) }
                    ?: "uploaded_file.fmu"
                val safeName = fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")

                // Reject if not .fmu (case insensitive)
                if (!safeName.lowercase().endsWith(".fmu")) {
                    BackendLogger.w("Non FMU file upload attempted: $safeName")
                    call.respondText(
                        "Only .fmu files are allowed",
                        status = HttpStatusCode.BadRequest
                    )
                    return@post
                }
                val channel: ByteReadChannel = call.receiveChannel()
                val bytes = channel.readRemaining().readByteArray()

                resourceManger.saveUpload(safeName, bytes)

                call.respondText("File '$safeName' salvato.", status = HttpStatusCode.OK)
            }
        }
    }
}
