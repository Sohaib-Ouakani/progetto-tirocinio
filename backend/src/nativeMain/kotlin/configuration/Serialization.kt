package configuration

import io.ktor.http.ContentType
import io.ktor.http.content.MultiPartData
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
   install(ContentNegotiation) {
      json()
       ignoreType<MultiPartData>()
   }
}
