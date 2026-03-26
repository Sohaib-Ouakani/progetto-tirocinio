import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun main() {
    hello("backend")
    println(helloFromFmuKt())
    prova()
}
