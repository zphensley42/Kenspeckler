package com.zhen.plugin;

import com.zhen.plugin.util.CryptoRun;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class KenspecklerPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        KenspecklerExtension extension = project.getExtensions().create("kenspeckler", KenspecklerExtension.class);

        Collection<File> sourceFiles = new ArrayList<>();

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.forEach(sourceSet -> {
            SourceDirectorySet sourceDirectorySet = sourceSet.getAllSource();
            sourceFiles.addAll(sourceDirectorySet.getFiles());
        });

        project.getTasks().register("listKenspeckleSourceFiles", com.zhen.plugin.ListSourceFilesTask.class, task -> {
            task.dependsOn(project.getTasks().getByName("assemble"));
            ListSourceFilesTask.Configuration cfg = new ListSourceFilesTask.Configuration();
            cfg.setSourceFiles(sourceFiles);
            task.configure(cfg);
            task.doLast(s -> System.out.println("Completed 'com.zhen.plugin.kenspeckler' listKenspeckleSourceFiles"));
        });

        project.getTasks().register("identifyKenspeckleStrings", com.zhen.plugin.ListKenspecklerStringsTask.class, task -> {
            task.dependsOn(project.getTasks().getByName("assemble"));
            MetaLoader.Configuration cfg = new MetaLoader.Configuration();
            cfg.setSourceFiles(sourceFiles);
            task.configure(cfg);
            task.doLast(s -> System.out.println("Completed 'com.zhen.plugin.kenspeckler' identifyKenspeckleStrings"));
        });

        // Next, make a task that uses the metas to modify the identified fields in the classes
        // Then, make a task that encrypts the strings for use in the newly output fields
        // Make a backup task to not lose the source
        // Make a cleanup task to kill backup
        // Make a task that generates the utility class that does the decryption at runtime


        project.getTasks().register("backupKenspeckler", com.zhen.plugin.BackupKenspeckleTask.class, task -> {
            MetaLoader.Configuration cfg = new MetaLoader.Configuration();
            cfg.setSourceFiles(sourceFiles);
            task.configure(cfg);
            task.doLast(s -> System.out.println("Completed 'com.zhen.plugin.kenspeckler' backupKenspeckler"));
        });

        project.getTasks().register("restoreKenspeckler", com.zhen.plugin.RestoreKenspeckleTask.class, task -> {
            task.dependsOn(project.getTasks().getByName("assemble"));
            MetaLoader.Configuration cfg = new MetaLoader.Configuration();
            cfg.setSourceFiles(sourceFiles);
            task.configure(cfg);
            task.doLast(s -> System.out.println("Completed 'com.zhen.plugin.kenspeckler' restoreKenspeckler"));
        });

        project.getTasks().register("buildKenspecklerSource", com.zhen.plugin.BuildKenspecklerSourceTask.class, task -> {
            task.dependsOn(project.getTasks().getByName("assemble"));
            MetaLoader.Configuration cfg = new MetaLoader.Configuration();

            String encFn = extension.getEncryptFn() == null ? "com.zhen.kenspeckler.util.AESEncryptFn" : extension.getEncryptFn();
            String decFn = extension.getDecryptFn() == null ? "com.zhen.kenspeckler.util.AESDecryptFn" : extension.getDecryptFn();
            cfg.setEncFn(encFn);
            cfg.setDecFn(decFn);

            cfg.setSourceFiles(sourceFiles);
            task.configure(cfg);
            task.doLast(s -> System.out.println("Completed 'com.zhen.plugin.kenspeckler' buildKenspecklerSource"));
        });

        project.getTasks().register("performStringKenspeckle", com.zhen.plugin.PerformStringKenspeckleTask.class, task -> {
            task.dependsOn(project.getTasks().getByName("assemble"));
            MetaLoader.Configuration cfg = new MetaLoader.Configuration();

            // TODO: Probably don't include the encrypt stuff in the source, only decrypt
            // TODO: Encrypt would be used in the build itself to encrypt the string -- how would this work for the extension function?
            // TODO: The source file would almost have to be copied over to our plugin project, built, then used in reflection -> encryption
            String encFn = extension.getEncryptFn() == null ? "com.zhen.kenspeckler.util.AESEncryptFn" : extension.getEncryptFn();
            String decFn = extension.getDecryptFn() == null ? "com.zhen.kenspeckler.util.AESDecryptFn" : extension.getDecryptFn();
            cfg.setEncFn(encFn);
            cfg.setDecFn(decFn);

            cfg.setSourceFiles(sourceFiles);
            task.configure(cfg);
            task.doLast(s -> System.out.println("Completed 'com.zhen.plugin.kenspeckler' performStringKenspeckle"));
        });
    }
}
