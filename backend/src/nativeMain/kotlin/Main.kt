import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import libfmi.*

@OptIn(ExperimentalForeignApi::class)
fun main() {
    hello("backend")

    val context: CPointer<fmi_import_context_t>? = fmi_import_allocate_context(null)
    val version = fmi_import_get_fmi_version(
        context,
        "",
        null
    )
}
