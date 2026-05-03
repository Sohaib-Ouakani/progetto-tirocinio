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
                post {
                    try {
                        fmuService.load(resourceManger.fmuPaths())
                    } catch (e: NoSuchElementException) {
                        BackendLogger.e("No FMU uploaded yet")
                        call.respondText(e.message ?: "No FMU uploaded", status = HttpStatusCode.BadRequest)
                        return@post
                    } catch (e: Exception) {
                        BackendLogger.e("Error initializing FMU: ${e.message}")
                        call.respondText(
                            "Error initializing FMU: ${e.message}",
                            status = HttpStatusCode.InternalServerError
                        )
                        return@post
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
                val header = call.request.header(HttpHeaders.ContentDisposition)
                    ?: run {
                        call.respondText(
                            "Missing Content-Disposition header with filename",
                            status = HttpStatusCode.BadRequest
                        )
                        return@post
                    }
                val fileName = ContentDisposition.parse(header)
                    .parameter(ContentDisposition.Parameters.FileName)
                    ?: run {
                        call.respondText(
                            "Filename not found in Content-Disposition header",
                            status = HttpStatusCode.BadRequest
                        )
                        return@post
                    }
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
