package com.zhen.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import javax.inject.Inject

open class PerformStringKenspeckleTask : DefaultTask {
    private var m_cfg: MetaLoader.Configuration? = null

    @Inject
    constructor() : super()

    fun configure(cfg : MetaLoader.Configuration) {
        m_cfg = cfg
    }

    fun encryptData(encryptMeta: KenspeckleClassMeta, target: KenspeckleClassMeta) {
        encryptMeta.cls?.let {cls ->


            target.kenspeckleStrings?.let { strs ->
                for(field in strs) {
                    // First, build our encrypter
                    val pType = ByteArray::class.java
                    val ctorMethod = cls.getConstructor(pType, pType)
                    val runMethod = cls.getMethod("run")

                    // TODO: Get our key from the config
                    val key = "1234567890".encodeToByteArray()

                    val ctors = target.cls!!.declaredConstructors
                    val ctor = ctors.filter {
                        it.genericParameterTypes.isEmpty()
                    }.firstOrNull()?.apply {
                        isAccessible = true
                    }

                    val fieldObj = ctor!!.newInstance()
                    field.isAccessible = true
                    val strVal = field.get(fieldObj) as String
                    val encrypter = ctorMethod.newInstance(key, strVal.encodeToByteArray())

                    // Then run the encryption method
                    val encrypted = runMethod.invoke(encrypter)
                    println("encrypted: $encrypted")

                    // TODO: Write this encrypted value out
                    // TODO: Use ClassReader to read the class and modify the value of the constants instead of this string parsing?
                    // TODO: (Then write the modified classes back out)
                }
            }
        }
    }

    @TaskAction
    fun execute() {
//        val reader = ClassReader()


        m_cfg?.let { cfg ->
            val metas = MetaLoader.INSTANCE.load(project, cfg)
            val encryptMeta = MetaLoader.INSTANCE.loadEncryptMeta(project, cfg)

            println("encryptMeta: $encryptMeta")

            encryptMeta?.let { eMeta ->
                for(meta in metas) {
                    encryptData(eMeta, meta)
                }
            }
        }
    }
}