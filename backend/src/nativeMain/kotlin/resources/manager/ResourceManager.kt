package resources.manager

import fmu.FmuPaths
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem


class ResourceManager(arg: String?) {
    val baseDir: Path = arg
        ?.let { Path(it).parent }          // executable path → its parent dir
        ?: SystemFileSystem.resolve(Path("."))  // fallback to CWD
    val resourceDir: Path = Path("$baseDir/resources/")
    val uploadDir = Path("$resourceDir/models/")
    val extractedDirPath = Path("$resourceDir/extracted")
    var fmuPath: Path? = null

    init {
        SystemFileSystem.createDirectories(resourceDir)
        SystemFileSystem.createDirectories(uploadDir)
        SystemFileSystem.createDirectories(extractedDirPath)
    }

    fun deleteRecursively(path: Path) {
        if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
            SystemFileSystem.list(path).forEach { deleteRecursively(it) }
        }
        SystemFileSystem.delete(path)
    }

    fun resetUploadDirectory(): Unit =
        SystemFileSystem.list(uploadDir)
            .filter { it.name.endsWith(".fmu", ignoreCase = true) }
            .forEach { SystemFileSystem.delete(it) }

    fun resetExtractedDirectory(): Unit =
        SystemFileSystem.list(extractedDirPath)
            .filter { SystemFileSystem.metadataOrNull(it)?.isDirectory == true || it.name.endsWith(".xml", ignoreCase = true) }
            .forEach { deleteRecursively(it) }

    fun findCurrentFmu(): Path? =
        SystemFileSystem.list(uploadDir)
            .firstOrNull { it.name.endsWith(".fmu", ignoreCase = true) }

    fun resetResourcesDirectory() {
        resetExtractedDirectory()
        resetUploadDirectory()
    }

    fun terminateResourcesDirectory() {
        deleteRecursively(resourceDir)
    }

    fun updateFmuPath() {
        fmuPath = findCurrentFmu()
    }

    fun fmuPaths(): FmuPaths {
        val path = fmuPath ?: error("No FMU uploaded yet")

        return FmuPaths(
            fmuPath = path.toString(),
            extractedDir = extractedDirPath.toString(),
            modelsDir = uploadDir.toString()
        )
    }
}
