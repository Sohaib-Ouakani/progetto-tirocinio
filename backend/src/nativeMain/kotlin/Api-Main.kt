import configuration.configureCors
import configuration.configureRouting
import configuration.configureSerialization
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun main(args: Array<String>) {
    val baseDir = if (args.isNotEmpty()) args[0] else "."

    embeddedServer(CIO, port = 8080) {
        configureCors()
        configureSerialization()
        configureRouting(baseDir)
    }.start(wait = true)
}
