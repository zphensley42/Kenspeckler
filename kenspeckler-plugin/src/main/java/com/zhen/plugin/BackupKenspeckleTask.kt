package com.zhen.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

open class BackupKenspeckleTask : DefaultTask {
    private val backupDirName = "kenspecklerBackup/"
    private var m_cfg: MetaLoader.Configuration? = null

    @Inject
    constructor() : super()

    fun configure(cfg : MetaLoader.Configuration) {
        m_cfg = cfg
    }

    @TaskAction
    fun execute() {
        m_cfg?.let { cfg ->
            val metas = MetaLoader.INSTANCE.load(project, cfg)

            val backupFiles = arrayListOf<File>()
            for(meta in metas) {
                meta.backup(project)?.let { bf ->
                    backupFiles.add(bf)
                }
            }

            printBackupFiles(backupFiles)
        }
    }

    private fun printBackupFiles(files: ArrayList<File>) {
        println("...Printing Backup Files...")
        for(f in files) {
            println("\tFile: ${f.absolutePath}")
        }
    }
}