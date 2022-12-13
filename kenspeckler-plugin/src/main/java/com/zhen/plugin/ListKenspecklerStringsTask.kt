package com.zhen.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class ListKenspecklerStringsTask : DefaultTask {

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
            printMetas(metas)
        }
    }

    private fun printMetas(metas: HashSet<KenspeckleClassMeta>) {
        println("...Printing Metas...")
        for(m in metas) {
            println("\tmeta: $m")
        }
    }
}