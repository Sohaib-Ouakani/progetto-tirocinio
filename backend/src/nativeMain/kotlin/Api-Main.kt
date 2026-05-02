import configuration.configureCors
import configuration.configureRouting
import fmu.Fmu
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import resources.manager.ResourceManager

fun main(args: Array<String>) {
    val arg = args.firstOrNull()
    val resourceManager = ResourceManager(arg)
    val fmuService = Fmu()

    val server = embeddedServer(CIO, port = 8080) {
        configureCors()
        configureRouting(resourceManager, fmuService)

        monitor.subscribe(ApplicationStopped) {
            resourceManager.terminateResourcesDirectory()
        }
    }

    server.addShutdownHook { resourceManager.terminateResourcesDirectory() }
    server.start(wait = true)
}
