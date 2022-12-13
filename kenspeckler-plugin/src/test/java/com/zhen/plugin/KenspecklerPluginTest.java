package com.zhen.plugin;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;


public class KenspecklerPluginTest {
    @Test
    public void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.zhen.plugin.kenspeckler");

        // Verify the result
        assertNotNull(project.getTasks().findByName("listKenspeckleSourceFiles"));
    }
}
