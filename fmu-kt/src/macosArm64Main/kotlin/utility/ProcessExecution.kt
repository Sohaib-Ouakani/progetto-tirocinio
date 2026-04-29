package utility

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system
const val BUFFER_SIZE = 4096

class ProcessExecution {
    fun run (vararg args: String): Int {
        val cmd = args.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }
        return system(cmd)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun runWithOutput(vararg args: String): String {
        val cmd = args.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }
        val sb: StringBuilder = StringBuilder()
        val pipe = popen(cmd, "r")
        memScoped {
            val buff = allocArray<ByteVar>(BUFFER_SIZE)
            while (fgets(buff, BUFFER_SIZE, pipe) != null) {
                sb.append(buff.toKString())
            }
        }
        pclose(pipe)
        return sb.toString().trim()
    }
}
