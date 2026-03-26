import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import libfmi.*

fun helloFromFmuKt(): String = "Hello from fmu-kt!"

@OptIn(ExperimentalForeignApi::class)
fun prova(): Unit {
    val context: CPointer<fmi_import_context_t>? = fmi_import_allocate_context(null)
    val version = fmi_import_get_fmi_version(
        context,
        "",
        null
    )
}
