package com.zhen.plugin

import org.gradle.api.Project
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class KenspeckleClassMeta(var sourceFile: File?, var classFile: File?, var cls: Class<*>?, var kenspeckleStrings : HashSet<Field>?) {
    private val backupDirName = "kenspecklerBackup/"

    fun backup(project: Project) : File? {
        var dest = File(project.projectDir, backupDirName)
        var packageName = cls?.packageName ?: run { "" }
        packageName = packageName.replace(".", "/")

        dest = File(dest, packageName)

        try {
            Files.createDirectories(dest.toPath())
        }
        catch (_ : FileAlreadyExistsException) {}

        var destFile : File? = null
        sourceFile?.let { src ->
            destFile = File(dest, src.name)
            Files.copy(src.toPath(), destFile!!.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return destFile
    }

    fun restore(project: Project) : File? {
        sourceFile?.let { srcFile ->
            var backupSource = File(project.projectDir, backupDirName)
            var packageName = cls?.packageName ?: run { "" }
            packageName = packageName.replace(".", "/")

            backupSource = File(backupSource, "$packageName/${srcFile.name}")

            if(backupSource.exists()) {
                val destFile = File(project.projectDir, "src/main/java/$packageName/${srcFile.name}")
                Files.copy(backupSource.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                return@restore destFile
            }
        }
        return null
    }

    fun replaceStrings() {

    }
}
