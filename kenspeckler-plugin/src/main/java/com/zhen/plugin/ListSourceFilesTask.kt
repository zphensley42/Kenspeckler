package com.zhen.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.inject.Inject

open class ListSourceFilesTask : DefaultTask {

    class Configuration {
        var sourceFiles: Collection<File>? = null
    }

    private var m_cfg: Configuration? = null
    private var m_parseClasses = ArrayList<Class<*>>()

    @Inject
    constructor() : super()

    fun configure(cfg: Configuration?) {
        m_cfg = cfg
    }

    // TODO: Store m_parseClasses somewhere for other tasks? Or just let other tasks do the work, something like that
    // TODO: Next task: begin running through each class, looking for strings to encrypt

    @TaskAction
    fun listFiles() {
        println("...Listing files...")
        val buildDir = project.buildDir
        val classes = ArrayList<Class<*>>()
        for (file in m_cfg!!.sourceFiles!!) {
            println("File: " + file.absolutePath)

            val fileRelativePath = file.absolutePath
                .replace(Regex(".*/src/main/java/"), "")
                .replace("/", ".")
                .replace(".java", "")
            val loc = File(buildDir, "classes/java/main/").toURI().toURL()

            val classFile = File(File(buildDir, "classes/java/main/"), fileRelativePath)
            val classFileExists = classFile.exists()

            val loadedClass = URLClassLoader.newInstance(arrayOf(loc)).loadClass(fileRelativePath)
            println("Loaded class: ${loadedClass.canonicalName}")

            classes.add(loadedClass)
        }

        m_parseClasses = classes
    }
}