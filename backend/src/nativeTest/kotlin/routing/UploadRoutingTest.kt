package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UploadRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private lateinit var rm: FakeResourceManager

    @BeforeTest
    fun setup() {
        rm = FakeResourceManager()
        server = embeddedServer(io.ktor.server.cio.CIO, port = 8192) {
            configureCors()
            configureRouting(rm, FakeFmuService())
        }
        server.start()
        client = HttpClient(io.ktor.client.engine.cio.CIO)
    }

    @AfterTest
    fun teardown() {
        client.close()
        server.stop(0, 0)
    }

    @Test
    fun `upload valid fmu returns 200`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"BouncingBall.fmu\"")
            setBody(ByteArray(64) { it.toByte() })
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `upload valid fmu saves correct filename`() = runBlocking {
        client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"BouncingBall.fmu\"")
            setBody(ByteArray(64) { it.toByte() })
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals("BouncingBall.fmu", rm.lastSavedFileName)
    }

    @Test
    fun `upload valid fmu saves correct bytes`() = runBlocking {
        val payload = ByteArray(64) { it.toByte() }
        client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"BouncingBall.fmu\"")
            setBody(payload)
            contentType(ContentType.Application.OctetStream)
        }
        assertNotNull(rm.lastSavedData)
        assertEquals(payload.size, rm.lastSavedData!!.size)
    }

    @Test
    fun `upload missing Content-Disposition header returns 400`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload missing Content-Disposition includes error message`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertTrue(response.bodyAsText().contains("Missing Content-Disposition"))
    }

    @Test
    fun `upload header without filename returns 400`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload header without filename includes error message`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertTrue(response.bodyAsText().contains("Filename not found"))
    }

    @Test
    fun `upload non-fmu file returns 400`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"model.zip\"")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload non-fmu file includes error message`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"model.zip\"")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertTrue(response.bodyAsText().contains("Only .fmu files are allowed"))
    }

    @Test
    fun `upload fmu with uppercase extension returns 200`() = runBlocking {
        val response = client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"Model.FMU\"")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `upload sanitizes path traversal characters in filename`() = runBlocking {
        client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"../evil/path.fmu\"")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        // path separators replaced with underscores
        assertEquals(".._evil_path.fmu", rm.lastSavedFileName)
    }

    @Test
    fun `upload sanitizes all dangerous characters`() = runBlocking {
        client.post("http://localhost:8192/fmi/upload") {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"my:model*.fmu\"")
            setBody(ByteArray(64))
            contentType(ContentType.Application.OctetStream)
        }
        assertEquals("my_model_.fmu", rm.lastSavedFileName)
    }
}
