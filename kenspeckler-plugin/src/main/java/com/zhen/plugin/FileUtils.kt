package com.zhen.plugin

import java.io.File

class FileUtils {
    companion object {
        fun filesInDir(dir: File, filter: (File) -> Boolean) : Set<File> {
            var tree = HashSet<File>()
            dir.listFiles()?.let { dirFiles ->
                for(file in dirFiles) {
                    if(filter(file)) {
                        if(file.isDirectory) {
                            tree.addAll(filesInDir(file, filter))
                        }
                        else {
                            tree.add(file)
                        }
                    }
                }
            }
            return tree
        }
    }
}