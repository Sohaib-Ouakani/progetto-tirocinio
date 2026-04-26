import configuration.configureCors
import configuration.configureRouting
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import utility.FilesystemManager

fun main(args: Array<String>) {
    val baseDir = if (args.isNotEmpty()) args[0] else "."

    embeddedServer(CIO, port = 8080) {
        configureCors()
        //configureSerialization()
        configureRouting(baseDir)
    }.start(wait = true)
}
