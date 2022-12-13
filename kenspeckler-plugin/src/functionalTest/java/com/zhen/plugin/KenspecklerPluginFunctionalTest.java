package com.zhen.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static org.junit.Assert.*;


public class KenspecklerPluginFunctionalTest {
    private File projectDir = new File("build/functionalTest");

    private void modifySourceFile() throws IOException {
        File sourcesDir = new File("build/functionalTest/src/main/java/com/zhen/kenspecklertest");
        writeString(new File(sourcesDir, "TestClass.java"), "package com.zhen.kenspecklertest; public class TestClass {}");
    }

    private String getSourceFileString() throws IOException {
        File sourcesDir = new File("build/functionalTest/src/main/java/com/zhen/kenspecklertest");
        byte[] sourceBytes = Files.readAllBytes(new File(sourcesDir, "TestClass.java").toPath());
        return new String(sourceBytes);
    }

    private String testEncryptFn = """
            package com.zhen.kenspecklertest;
                        
            import java.nio.ByteBuffer;
            import java.util.Base64;
                        
            public class B64EncryptFn {
                private final byte[] k;
                private final byte[] t;
                public B64EncryptFn(byte[] k, byte[] t) {
                    this.k = k;
                    this.t = t;
                }
                        
                public byte[] run() {
                    byte[] data = concat(k, t);
                    return Base64.getEncoder().encode(data);
                }
                        
                byte[] concat(byte[] a, byte[] b) {
                        
                    byte[] c = new byte[a.length + b.length + 8];
                    System.arraycopy(a, 0, c, 0, a.length);
                    System.arraycopy(b, 0, c, a.length, b.length);
                        
                    ByteBuffer bb = ByteBuffer.allocate(8);
                    bb.putInt(a.length);
                    bb.putInt(b.length);
                    System.arraycopy(bb.array(), 0, c, a.length + b.length, 8);
                        
                    return c;
                }
            }
            """;

    private String testDecryptFn = """
            package com.zhen.kenspecklertest;
                        
            import java.nio.ByteBuffer;
            import java.nio.charset.StandardCharsets;
            import java.util.Base64;
                        
            public class B64DecryptFn {
                private final byte[] k;
                private final byte[] t;
                public B64DecryptFn(byte[] k, byte[] t) {
                    this.k = k;
                    this.t = t;
                }
                        
                public byte[] run() {
                    byte[] data = Base64.getDecoder().decode(t);
                        
                    // parse the parts
                    byte[] kSize = new byte[4];
                    byte[] tSize = new byte[4];
                    System.arraycopy(data, data.length - 8, kSize, 0, 4);
                    System.arraycopy(data, data.length - 4, tSize, 0, 4);
                        
                    ByteBuffer kBb = ByteBuffer.allocate(4);
                    kBb.put(kSize);
                    ByteBuffer tBb = ByteBuffer.allocate(4);
                    tBb.put(tSize);
                        
                    byte[] key = new byte[kBb.getInt()];
                    byte[] text = new byte[tBb.getInt()];
                    System.arraycopy(data, 0, k, 0, key.length);
                    System.arraycopy(data, key.length, t, 0, text.length);
                        
                    String kStr = new String(k, StandardCharsets.UTF_8);
                    String keyStr = new String(key, StandardCharsets.UTF_8);
                    if(!keyStr.equals(kStr)) {
                        throw new RuntimeException("Keys do not match!");
                    }
                        
                    return text;
                }
            }
            """;

    private void setupBuild() throws IOException {
        // Setup the test build
        Files.createDirectories(projectDir.toPath());

        // Setup some sources
        File sourcesDir = new File("build/functionalTest/src/main/java/com/zhen/kenspecklertest");
        Files.createDirectories(sourcesDir.toPath());
        writeString(new File(sourcesDir, "B64EncryptFn.java"), testEncryptFn);
        writeString(new File(sourcesDir, "B64DecryptFn.java"), testDecryptFn);
        writeString(new File(sourcesDir, "TestClass.java"),
                """
                        package com.zhen.kenspecklertest;
                        
                        public class TestClass {
                            private String pwd = "some_secret_password";
                            
                            TestClass() {}
                        }
                        """);

        // Setup some gradle project settings
        writeString(new File(projectDir, "settings.gradle"), "");
        writeString(new File(projectDir, "build.gradle"),
                """
                        plugins {
                            id('java')
                            id('com.zhen.plugin.kenspeckler')
                        }
                        
                        kenspeckler.encryptFn = "com.zhen.kenspecklertest.B64EncryptFn"
                        kenspeckler.decryptFn = "com.zhen.kenspecklertest.B64DecryptFn"
                        
                        sourceSets {
                            main {
                                java {
                                    allSource
                                }
                            }
                        }
                        """);
    }

    @Test
    public void testListKenspeckleSourceFiles() throws IOException {
        setupBuild();

        // Run the build
        BuildResult result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("listKenspeckleSourceFiles")
            .withProjectDir(projectDir)
            .withDebug(true)
            .build();

        // Verify the result
        assertTrue(result.getOutput().contains("Completed 'com.zhen.plugin.kenspeckler' listKenspeckleSourceFiles"));
    }

    @Test
    public void testListKenspecklerStrings() throws IOException {
        setupBuild();

        // Run the build
        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("identifyKenspeckleStrings")
                .withProjectDir(projectDir)
                .withDebug(true)
                .build();

        // Verify the result
        assertTrue(result.getOutput().contains("Completed 'com.zhen.plugin.kenspeckler' identifyKenspeckleStrings"));
    }

    @Test
    public void testBackupKenspeckler() throws IOException {
        setupBuild();

        // Run the build
        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("backupKenspeckler")
                .withProjectDir(projectDir)
                .withDebug(true)
                .build();

        // Verify the result
        assertTrue(result.getOutput().contains("Completed 'com.zhen.plugin.kenspeckler' backupKenspeckler"));
    }

    @Test
    public void testRestoreKenspeckler() throws IOException {
        setupBuild();

        // Modify the source files
        modifySourceFile();

        String sourceFileString = getSourceFileString();
        assertEquals(sourceFileString, "package com.zhen.kenspecklertest; public class TestClass {}");


        // Run the build (restore0
        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("restoreKenspeckler")
                .withProjectDir(projectDir)
                .withDebug(true)
                .build();

        // Verify the result
        assertTrue(result.getOutput().contains("Completed 'com.zhen.plugin.kenspeckler' restoreKenspeckler"));

        // Ensure the original source files match the backup source files
        sourceFileString = getSourceFileString();
        assertNotEquals(sourceFileString, "package com.zhen.kenspecklertest; public class TestClass {}");
    }

    @Test
    public void testBuildKenspecklerSource() throws IOException {
        setupBuild();

        // Run the build
        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("buildKenspecklerSource")
                .withProjectDir(projectDir)
                .withDebug(true)
                .build();

        // Verify the result
        assertTrue(result.getOutput().contains("Completed 'com.zhen.plugin.kenspeckler' buildKenspecklerSource"));
    }

    @Test
    public void testPerformStringKenspeckle() throws IOException {
        setupBuild();

        // Run the build
        BuildResult result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("performStringKenspeckle")
                .withProjectDir(projectDir)
                .withDebug(true)
                .build();

        // Verify the result
        assertTrue(result.getOutput().contains("Completed 'com.zhen.plugin.kenspeckler' performStringKenspeckle"));
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
