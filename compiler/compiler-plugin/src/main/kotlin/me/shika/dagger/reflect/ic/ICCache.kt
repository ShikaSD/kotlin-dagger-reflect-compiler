package me.shika.dagger.reflect.ic

import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiManagerImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import java.io.File
import java.io.FileOutputStream

data class ICCache(
    private val project: Project,
    private val module: ModuleDescriptor,
    private val manifestDir: File?
) {
    private val manifestFile = manifestDir?.let { File(it, "compiled-manifest.txt") }
    private val entries: Set<ICEntry> = readManifest()

    private val newFiles = mutableSetOf<ICEntry>()

    init {
        manifestDir?.mkdirs()
    }

    private fun readManifest(): Set<ICEntry> =
        if (manifestFile != null && manifestFile.exists()) {
            manifestFile.bufferedReader().useLines {
                it.map { line ->
                    val chunks = line.split(":")
                    val path = chunks[0]
                    val pkg = chunks[1]
                    val classNames = chunks[2]
                    val classId = ClassId(FqName(pkg), FqName(classNames), false)
                    ICEntry(classId, path)
                }.toSet()
            }
        } else {
            emptySet()
        }

    fun recordGeneratedFile(classId: ClassId, clsPath: String) {
        newFiles += ICEntry(
            classId,
            clsPath
        )
    }

    fun recordChanges(files: MutableCollection<KtFile>): List<File> {
        val toDelete = entries.filter { module.findClassAcrossModuleDependencies(it.classId) == null }.map { it.compiledFilePath }
        toDelete.forEach { File(it).delete() }
        files.removeAll { it.virtualFilePath in toDelete }

        project.clearCachedOutputFiles(files, newFiles)

        val output = (entries.filter { it.compiledFilePath !in toDelete } + newFiles).toSet()
        if (manifestFile != null) {
            manifestFile.createNewFile()
            FileOutputStream(manifestFile, false).bufferedWriter().use {
                output.forEach { entry ->
                    val formatted = "${entry.compiledFilePath}:${entry.classId.packageFqName.asString()}:${entry.classId.relativeClassName.asString()}"
                    it.write(formatted)
                    it.write("\n")
                }
                it.flush()
            }
        }

        return newFiles
            .filter { it !in entries }
            .map { File(it.compiledFilePath) }
            .distinct()
    }

    private fun Project.clearCachedOutputFiles(files: MutableCollection<KtFile>, newFiles: Set<ICEntry>) {
        files.removeIf { file ->
            newFiles.any { it.compiledFilePath == file.virtualFilePath }.also { removed ->
                if (removed) {
                    dropFileCaches(file)
                }
            }
        }
    }

    private fun Project.dropFileCaches(file: KtFile) {
        (PsiManager.getInstance(this) as PsiManagerImpl).fileManager.setViewProvider(file.virtualFile, null)
    }
}

data class ICEntry(
    val classId: ClassId,
    val compiledFilePath: String
)
