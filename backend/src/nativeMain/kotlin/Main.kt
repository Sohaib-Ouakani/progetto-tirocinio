import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun nomain() {
    hello("backend")
    println(helloFromFmuKt())
    prova()
}
