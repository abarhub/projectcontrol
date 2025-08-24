package org.projectcontrol.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PomParserServiceTest {

    @TempDir
    private Path tempDir;

    @Spy
    private XmlParserService xmlParserService;

    @InjectMocks
    private PomParserService pomParserService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void updateVersion() throws Exception {
        // ARRANGE
        var version = "1.0.0";
        var s = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.5.4</version>
                        <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    <groupId>org.projectix</groupId>
                    <artifactId>projectix-parent</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <name>projectix-parent</name>
                    <packaging>pom</packaging>
                    <description>Demo project for Spring Boot</description></project>""";
        var pomFile = tempDir.resolve("pom.xml");
//        var s02=Files.readString(Path.of("../pom.xml"));
        //s=s02;
        Files.write(pomFile, s.getBytes(StandardCharsets.UTF_8));


        // ACT
        pomParserService.updateVersion(pomFile, version, false, null);

        // ASSERT
        var s2 = Files.readString(pomFile);
        assertTrue(s2.contains(version), () -> s2);
        var ref = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.5.4</version>
                        <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    <groupId>org.projectix</groupId>
                    <artifactId>projectix-parent</artifactId>
                    <version>1.0.0</version>
                    <name>projectix-parent</name>
                    <packaging>pom</packaging>
                    <description>Demo project for Spring Boot</description></project>""";
        assertEquals(ref, s2);
    }
}