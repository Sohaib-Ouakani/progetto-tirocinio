package configuration

import io.ktor.server.application.*
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.routing.*
import io.ktor.server.response.*
import fmu.Fmu
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

fun Application.configureRouting(baseDir: String) {
    //val fmuPath = "$baseDir/resources/models/BouncingBall.fmu"
    val uploadDir = Path("$baseDir/resources/models/")
    var fmuPath: Path? = null
    val extractedDirPath = Path("$baseDir/resources/extracted")
    //val extractedPath = "$baseDir/resources/extracted"
    fun findCurrentFmu(): Path? =
        SystemFileSystem.list(uploadDir)
            .firstOrNull { it.name.endsWith(".fmu", ignoreCase = true) }

    var fmu: Fmu? = null

    install(createApplicationPlugin("FmuCleanup") {
        on(MonitoringEvent(ApplicationStopped)) {
            fmu?.close()
            fmu = null
        }
    })


    routing {
        get("/health") { call.respondText("OK") }
        get("/") { call.respondText("Welcome to the home of the api") }

        route("/fmi") {
            route("/init") {
                install(ContentNegotiation) { json() }
                get {
                    try {
                        if(fmuPath == null) {
                            return@get call.respondText("Error upload FMU first", status = HttpStatusCode.BadRequest)
                        }
                        fmu?.close() // Close previous FMU if it exists
                        fmu = Fmu(fmuPath.toString(), extractedDirPath.toString())
                    } catch (e: Exception) {
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
                install(ContentNegotiation) { json() }
                get {
                    val currentFmu = fmu ?: return@get call.respondText("please initiate...", status = HttpStatusCode.BadRequest)
                    try {
                        val result = currentFmu.fmuInfo
                        call.respond(result)
                    } finally {
                        currentFmu.close()
                        fmu = null
                    }
                }
            }

            post("/upload") {
                val fileName = call.request.header(HttpHeaders.ContentDisposition)
                    ?.let { ContentDisposition.parse(it).parameter(ContentDisposition.Parameters.FileName) }
                    ?: "uploaded_file"
                val safeName = fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")

                // Delete all existing .fmu files before saving the new one
                SystemFileSystem.list(uploadDir)
                    .filter { it.name.endsWith(".fmu", ignoreCase = true) }
                    .forEach { SystemFileSystem.delete(it) }

                val filePath = Path(uploadDir, safeName)

                val channel: ByteReadChannel = call.receiveChannel()
                val bytes = channel.readRemaining().readByteArray()

                SystemFileSystem.sink(filePath).buffered().use { it.write(bytes) }

                fmuPath = findCurrentFmu()

                call.respondText("File '$safeName' salvato.", status = HttpStatusCode.OK)
            }
        }
    }
}
