package recompiler

import platform.posix.F_OK
import platform.posix.access
import platform.posix.open
import utility.ClangComplier
import utility.DescriptionParser
import utility.FilesystemManager
import utility.FmiHeaderSynthesiser
import utility.FmuPackager
import utility.ProcessExecution

actual class FmuRecompiler {
    private val exec = ProcessExecution()
    private val fs = FilesystemManager()
    private val packager = FmuPackager(exec, fs)
    private val complier = ClangComplier(exec)
    private val headers = FmiHeaderSynthesiser(fs)

    private val fmiPrefix = "fmi2"
    private val targets = listOf("arm64-apple-macos11", "x86_64-apple-macos10.13")

    private fun discoverAliasFlags(nm: String, objs: List<String>, modelId: String, fmiPrefix: String): List<String> =
        objs.flatMap { obj ->
            exec.runWithOutput(nm, "--defined-only", obj)
                .lines()
                .mapNotNull { Regex("""[_]?(${modelId}_${fmiPrefix}[A-Za-z]+)""").find(it)?.groupValues?.get(1) }
                .toSet()
                .map { raw ->
                    val suffix = raw.removePrefix("${modelId}_")
                    "-Wl,-alias,_$raw,_$suffix"
                }
        }.distinct()

    actual fun recompile(inputFmu: String, outputFmu: String) {
        val input = fs.pathAbsolute(inputFmu)
        val output = fs.pathAbsolute(outputFmu)
        check(fs.fileExists(input)) { "FMU not found: $input" }

        packager.withTempDir { tmp ->
            val extracted = "$tmp/extracted"
            packager.extract(input, extracted)

            val xml = fs.readFile("$extracted/modelDescription.xml")
            val parser = DescriptionParser(xml)
            val modelId  = parser.findModelId()
            val sourcesDir = "$extracted/sources"

            check(access(sourcesDir, F_OK) == 0) { "No source folder, precompiled FMU" }

            headers.synthesise(sourcesDir)

            val sources = parser.findSourceFiles().map { "$sourcesDir/$it" }

            val objsByArch = targets.associateWith { target ->
                val arch = target.substringBefore("-")
                complier.compileObjects(
                    sources = sources,
                    target = target,
                    includeDir = sourcesDir,
                    outDir = "$tmp/obj_$arch"
                )
            }

            // Alias of symbols of arm64 are the same of x86_64, that's why .first()
            val aliasFlags = complier.discoverAliasFlags(
                objs = objsByArch.values.first(),
                modelId = modelId,
                fmiPrefix = fmiPrefix
            )

            val slices = targets.map { target ->
                val arch  = target.substringBefore("-")
                val slice = "$tmp/$arch.dylib"

                complier.linkDylib(
                    objs = objsByArch[target]!!,
                    target = target,
                    aliasFlags = aliasFlags,
                    out = slice
                )
                slice
            }

            val universal = "$tmp/$modelId.dylib"
            complier.createUniversalBinary(
                slices = slices,
                out = universal
            )

            val binDest = "$extracted/binaries/darwin64"
            exec.run("mkdir", "-p", binDest)
            exec.run("cp", universal, "$binDest/$modelId.dylib")

            packager.zip(binDest, output)

            println("Recompilation successful: $output")
        }
    }

    fun recompile2(inputFmu: String, outputFmu: String) {
        val input = fs.pathAbsolute(inputFmu)
        val output = fs.pathAbsolute(outputFmu)
        check(fs.fileExists(input)) { "FMU not found: $input" }

        val tmp = exec.runWithOutput("mktemp", "-d", "/tmp/fmu_recompile_XXXXXX")
            .also { check(it.isNotBlank()) { "temp folder creation failed" } }
        val extracted = "$tmp/extracted"
        exec.run("mkdir", "-p", extracted)


        try {
            check(exec.run("unzip", "-q", input, "-d", extracted) == 0) { "Extraction failed" }

            val xml = fs.readFile("$extracted/modelDescription.xml")
            val parser = DescriptionParser(xml)
            val modelId  = parser.findModelId()
            val fmiPrefix = "fmi2"

            val sourcesDir = "$extracted/sources"
            check(access(sourcesDir, F_OK) == 0) { "No source folder, precompiled FMU" }
            FmiHeaderSynthesiser(fs).synthesise(sourcesDir)

            val sources = parser.findSourceFiles().map { "$sourcesDir/$it" }

            val targets = listOf("arm64-apple-macos11", "x86_64-apple-macos10.13")
            val objsByArch = targets.associateWith { target ->
                val arch   = target.substringBefore("-")
                val objDir = "$tmp/obj_$arch".also { exec.run("mkdir", "-p", it) }
                sources.map { src ->
                    val obj = "$objDir/${src.substringAfterLast('/').removeSuffix(".c")}.o"
                    check(exec.run(
                        "clang",
                        "--target=$target",
                        "-c",
                        "-fPIC",
                        "-O2",
                        "-I$sourcesDir",
                        src,
                        "-o",
                        obj
                    ) == 0) { "Compilation failed: $src [$arch]" }
                    obj
                }
            }

            // Alias dai simboli arm64 (sono gli stessi per entrambe le arch)
            val aliasFlags = discoverAliasFlags("nm", objsByArch.values.first(), modelId, fmiPrefix)

            val slices = targets.map { target ->
                val arch  = target.substringBefore("-")
                val objs  = objsByArch[target]!!
                val slice = "$tmp/$arch.dylib"
                check(exec.run(
                    "clang",
                    "--target=$target",
                    "-dynamiclib",
                    "-fPIC",
                    *aliasFlags.toTypedArray(),
                    *objs.toTypedArray(),
                    "-lm",
                    "-Wl,-undefined,dynamic_lookup",
                    "-o", slice
                ) == 0) { "Link fallito [$arch]" }
                slice
            }

            val universal = "$tmp/$modelId.dylib"
            check(exec.run("lipo", "-create", *slices.toTypedArray(), "-output", universal) == 0)
            { "lipo failed" }

            val binDest = "$extracted/binaries/darwin64".also { exec.run("mkdir", "-p", it) }
            exec.run("cp", universal, "$binDest/$modelId.dylib")

            check(exec.run("cd", "\"$extracted\"", "&&", "zip", "-qr", "\"$output\"", ".") == 0)
            { "zip failed" }

            println("Recompilation successful: $output")

        } finally {
            exec.run("rm", "-rf", tmp)
        }
    }
}
