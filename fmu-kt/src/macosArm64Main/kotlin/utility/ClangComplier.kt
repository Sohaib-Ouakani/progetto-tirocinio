package utility

class ClangComplier(
    private val exec: ProcessExecution
) {
    fun compileObjects(
        sources: List<String>,
        target: String,
        includeDir: String,
        outDir: String
    ): List<String> {
        exec.run("mkdir", "-p", outDir)
        return sources.map { src ->
            val obj = "$outDir/${src.substringAfterLast('/').removeSuffix(".c")}.o"
            check(
                exec.run(
                    "clang",
                    "--target=$target",
                    "-c", "-fPIC", "-O2",
                    "-I$includeDir",
                    src, "-o", obj
                ) == 0
            ) { "Compilation failed: $src [${target.substringBefore("-")}]" }
            obj
        }
    }

    fun linkDylib(
        objs: List<String>,
        target: String,
        aliasFlags: List<String>,
        out: String
    ) {
        check(
            exec.run(
                "clang",
                "--target=$target",
                "-dynamiclib", "-fPIC",
                *aliasFlags.toTypedArray(),
                *objs.toTypedArray(),
                "-lm",
                "-Wl,-undefined,dynamic_lookup",
                "-o", out
            ) == 0
        ) { "Link failed [${target.substringBefore("-")}]" }
    }

    fun createUniversalBinary(slices:  List<String>, out: String) {
        check(exec.run("lipo", "-create", *slices.toTypedArray(), "-output", out) == 0) {
            "lipo failed"
        }
    }

    fun discoverAliasFlags(objs: List<String>, modelId: String, fmiPrefix: String): List<String> =
        objs.flatMap { obj ->
            exec.runWithOutput("nm", "--defined-only", obj)
                .lines()
                .mapNotNull {
                    Regex("""[_]?(${modelId}_${fmiPrefix}[A-Za-z]+)""")
                        .find(it)?.groupValues?.get(1)
                }
                .toSet()
                .map { raw ->
                    val suffix = raw.removePrefix("${modelId}_")
                    "-Wl,-alias,_$raw,_$suffix"
                }
        }.distinct()
}
