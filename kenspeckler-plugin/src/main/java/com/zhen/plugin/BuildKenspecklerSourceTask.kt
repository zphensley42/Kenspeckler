package com.zhen.plugin

import com.zhen.plugin.util.CryptoRun
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject

open class BuildKenspecklerSourceTask : DefaultTask {
    private val backupDirName = "kenspecklerBackup/"
    private var m_cfg: MetaLoader.Configuration? = null

    private val encrypterSourceTemplate = """
        package %PACKAGE%;
        
        class Kenspeckler {
            public static byte[] kenspeckle(byte[] key, byte[] text) {
                return new %IMPORT_ENC_FN%(key, text).run();
            }
            
            public static byte[] reverseKenspeckle(byte[] key, byte[] data) {
                return new %IMPORT_DEC_FN%(key, data).run();
            }      
        }  
    """.trimIndent()

    @Inject
    constructor() : super()

    fun configure(cfg : MetaLoader.Configuration) {
        m_cfg = cfg
    }

    @TaskAction
    fun execute() {
        val encFn = m_cfg?.encFn ?: run {"com.zhen.kenspeckler.util.AESEncryptFn"}
        val decFn = m_cfg?.decFn ?: run {"com.zhen.kenspeckler.util.AESDecryptFn"}

        val template = encrypterSourceTemplate
            .replace("%PACKAGE%", "com.zhen.kenspeckler.util")
            .replace("%IMPORT_ENC_FN%", encFn)
            .replace("%IMPORT_DEC_FN%", decFn)

        val destTemplateFile = File(project.projectDir, "src/main/java/com/zhen/kenspeckler/util/Kenspeckler.java")
        writeString(destTemplateFile, template)

        if(encFn == "com.zhen.kenspeckler.util.AESEncryptFn") {
            writeString(File(project.projectDir, "src/main/java/" + encFn.replace(".", "/") + ".java"), CryptoRun.defaultEncryptFn.replace("%PACKAGE%", "com.zhen.kenspeckler.util"))
        }
        if(decFn == "com.zhen.kenspeckler.util.AESDecryptFn") {
            writeString(File(project.projectDir, "src/main/java/" + decFn.replace(".", "/") + ".java"), CryptoRun.defaultDecryptFn.replace("%PACKAGE%", "com.zhen.kenspeckler.util"))
        }
    }

    @Throws(IOException::class)
    private fun writeString(file: File, string: String) {
        try {
            Files.createDirectories(file.parentFile.toPath())
        }
        catch(_ : FileAlreadyExistsException) {}
        FileWriter(file).use { writer -> writer.write(string) }
    }
}