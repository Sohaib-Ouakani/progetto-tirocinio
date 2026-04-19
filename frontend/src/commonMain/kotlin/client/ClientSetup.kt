package com.example.fmi_client.client

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object ApiClient {
    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    val rawClient = HttpClient()
}


suspend fun initApi(): String {
    val response = ApiClient.client.get("http://localhost:8080/fmi/init").bodyAsText()

    return response
}
suspend fun fetchInfo(): JsonObject {
    initApi()
    val jsonString =  ApiClient.client.get("http://localhost:8080/fmi/info").bodyAsText()
    val json = Json.parseToJsonElement(jsonString)

    return json.jsonObject
}

suspend fun uploadFile(file: PlatformFile) {
    val bytes = file.readBytes()
    ApiClient.rawClient.post("http://localhost:8080/fmi/upload") {
        header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
        setBody(bytes)
        contentType(ContentType.Application.OctetStream)
    }
}
