plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
    id("java")
}

group = "com.zhen"
version = "1.0"

repositories {
    // Use Maven Central for resolving dependencies
    mavenCentral()
}

dependencies {
    // Use JUnit test framework for unit tests
    testImplementation("junit:junit:4.13")
    implementation("org.ow2.asm:asm:9.4")
}

gradlePlugin {
    // Define the plugin
    val kenspeckler by plugins.creating {
        id = "com.zhen.plugin.kenspeckler"
        displayName = "Kenspeckler"
        description = "Source String Encrypter"
        tags.set(listOf("encrypt", "reflection", "source", "generation"))
        implementationClass = "com.zhen.plugin.KenspecklerPlugin"
    }

    website.set("https://github.com/zphensley42")
    vcsUrl.set("https://github.com/zphensley42.git")
}

publishing {
    repositories {
        maven {
            name = "localPluginRepo"
            url = uri("../local-plugin-repo")
        }
    }
}

// Add a source set and a task for a functional test suite
val functionalTest by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTestTask)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}
