package com.zhen.plugin

import org.gradle.api.Project
import java.io.File
import java.net.URLClassLoader

enum class MetaLoader {
    INSTANCE;

    class Configuration {
        var sourceFiles: Collection<File>? = null
        var encFn = ""
        var decFn = ""
    }

    private var m_parseMeta = HashSet<KenspeckleClassMeta>()

    fun load(project: Project, cfg: Configuration): HashSet<KenspeckleClassMeta> {
        loadMetas(project, cfg)
        loadStringFields()

        return m_parseMeta
    }

    fun loadEncryptMeta(project : Project, cfg: Configuration) : KenspeckleClassMeta? {
        if(m_parseMeta.isEmpty()) {
            loadMetas(project, cfg)
        }

        for(meta in m_parseMeta) {
            meta.cls?.let { cls ->
                val comp = "${cls.packageName}.${meta.sourceFile!!.name}".replace(".java", "")
                if(comp == cfg.encFn) {
                    return@loadEncryptMeta meta
                }
            }
        }
        return null
    }

    private fun loadMetas(project: Project, cfg: Configuration) {
        println("...Loading Metas...")
        m_parseMeta.clear()

        val buildDir = project.buildDir
        for (file in cfg.sourceFiles!!) {
            println("File: " + file.absolutePath)

            val classRelativePath = file.absolutePath
                .replace(Regex(".*/src/main/java/"), "")
                .replace(".java", ".class")

            val classLoaderRelativePath = file.absolutePath
                .replace(Regex(".*/src/main/java/"), "")
                .replace("/", ".")
                .replace(".java", "")
            val loc = File(buildDir, "classes/java/main/").toURI().toURL()

            val loadedClass = URLClassLoader.newInstance(arrayOf(loc)).loadClass(classLoaderRelativePath)
            println("Loaded class: ${loadedClass.canonicalName}")
            m_parseMeta.add(
                KenspeckleClassMeta(file, File(File(buildDir, "classes/java/main/"), classRelativePath), loadedClass, HashSet())
            )
        }
    }

    private fun loadStringFields() {
        println("...Loading String Fields...")
        for(meta in m_parseMeta) {
            meta.apply {
                cls?.let { c ->
                    kenspeckleStrings?.addAll(c.declaredFields.filter { it.type == String::class.java })
                }
            }

            meta.kenspeckleStrings?.forEach { f ->
                println("\tparsed field ${f.toGenericString()} in class: ${meta.cls?.canonicalName ?: let { "Null Class" } }")
            }
        }
    }

}