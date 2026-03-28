package com.example.fmi_client.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
