package com.zhen.plugin

import com.zhen.plugin.util.CryptoRun
import groovyjarjarasm.asm.Opcodes.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
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

    class EncryptFnMethod : MethodVisitor {
        val target : MethodVisitor?
        constructor(api: Int, methodVisitor: MethodVisitor?) : super(api, methodVisitor) {
            target = methodVisitor
        }
        override fun visitCode() {
            target?.let { t ->
                t.visitCode()
                t.visitEnd()
            }

        }
    }

    private fun writeDefaultEncryptFnClass(file: File) {
        val writer = ClassWriter(0)

        writer.visit(V1_5, ACC_PUBLIC, "com/zhen/kenspeckler/util/AESEncryptFn2", null, "java/lang/Object", null)
        writer.visitField(ACC_PRIVATE or ACC_FINAL, "k", "[B", null, null)
        writer.visitField(ACC_PRIVATE or ACC_FINAL, "t", "[B", null, null)

        val mv = writer.visitMethod(ACC_PUBLIC, "<init>", "([B[B)[Lcom/zhen/kenspeckler/util/AESEncryptFn2;", null, null)
        mv.visitCode()
        mv.visitVarInsn(ALOAD, 0)   // load self
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        mv.visitVarInsn(ALOAD, 0)   // load self
        mv.visitVarInsn(ALOAD, 1)   // load param1
        mv.visitFieldInsn(PUTFIELD, "com/zhen/kenspeckler/util/AESEncryptFn2", "k", "[B")   // putfield, indexbyte1, indexbyte2
        mv.visitVarInsn(ALOAD, 0)   // load self
        mv.visitVarInsn(ALOAD, 2)   // load param2
        mv.visitFieldInsn(PUTFIELD, "com/zhen/kenspeckler/util/AESEncryptFn2", "t", "[B")   // putfield, indexbyte1, indexbyte2
        mv.visitMaxs(3, 3)
        mv.visitEnd()

        val mv1 = writer.visitMethod(ACC_PUBLIC, "run", "()[B", null, null)
        mv1.visitCode()
        mv1.visitVarInsn(ALOAD, 0)
        mv1.visitFieldInsn(GETFIELD, "com/zhen/kenspeckler/util/AESEncryptFn2", "k", "[B")
        mv1.visitInsn(ARETURN)
        mv1.visitMaxs(1, 1)
        mv1.visitEnd()

        writeBytes(file, writer.toByteArray())
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

        // TODO: This is a test for using ASM to generate classes vs generating source and letting the compiler run
        // Honestly, the best way would be to just provide encrypted source that is decrypted and JIT'd to bytecode at runtime, but that doesn't seem
        // possible in closed platforms that separate executable pages (like Android)
        val encryptSourceFile = File(project.buildDir, "classes/java/main/com/zhen/kenspeckler/util/AESEncryptFn2.class")
        writeDefaultEncryptFnClass(encryptSourceFile)
    }

    private fun writeBytes(file: File, bytes: ByteArray) {
        Files.write(file.toPath(), bytes)
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