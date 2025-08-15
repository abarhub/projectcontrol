package org.projectcontrol.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.projectcontrol.core.vo.Position;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.*;

class XmlParserServiceTest {

    @TempDir
    private Path tempDir;

    private XmlParserService xmlParserService = new XmlParserService();

    @BeforeEach
    void setUp() {
    }

    @Test
    void parse() throws Exception {
        // ARRANGE
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
                    <description>Demo project for Spring Boot</description>
                    </project>""";
        var pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, s);
        var liste = List.of(List.of("project", "version"));

        // ACT
        var resultat = xmlParserService.parse(pomFile, liste);

        // ASSERT
        assertNotNull(resultat);
        assertEquals(1, resultat.size());
        assertEquals("0.0.1-SNAPSHOT", resultat.getFirst().valeur());
        assertThat(resultat)
                .hasSize(1)
                .extracting("balises", "valeur", "positionDebut", "positionFin")
                .contains(tuple(List.of("project", "version"), "0.0.1-SNAPSHOT",
                        new Position(13, 14, 640), new Position(13, 27, 653)));
    }


    @Test
    void parse2() throws Exception {
        // ARRANGE
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
                    <description>Demo project for Spring Boot</description>
                    </project>""";
        var pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, s);
        var liste = List.of(List.of("project", "description"));

        // ACT
        var resultat = xmlParserService.parse(pomFile, liste);

        // ASSERT
        assertNotNull(resultat);
        assertEquals(1, resultat.size());
        assertEquals("Demo project for Spring Boot", resultat.getFirst().valeur());
        assertThat(resultat)
                .hasSize(1)
                .extracting("balises", "valeur", "positionDebut", "positionFin")
                .contains(tuple(List.of("project", "description"), "Demo project for Spring Boot",
                        new Position(16, 18, 761), new Position(16, 45, 788)));
    }

    @Test
    void modifierFichier() throws Exception {
        // ARRANGE
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
                    <description>Demo project for Spring Boot</description>
                    </project>""";
        var pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, s);
        var liste = List.of(List.of("project", "version"));
//        Position debut = new Position(13, 30, 640);
//        Position fin = new Position(13, 43, 653);
        Position debut = new Position(13, 14, 640);
        Position fin = new Position(13, 27, 653);
        String nouvelleVersion = "1.0.0";

        // ACT
//        xmlParserService.modifierFichier(pomFile.toString(),debut,fin,nouvelleVersion);
        xmlParserService.modifierFichier2(pomFile.toString(),debut,fin,nouvelleVersion);

        // ASSERT
        var s2 = Files.readString(pomFile);
        assertTrue(s2.contains(nouvelleVersion), () -> s2);
        var ref="""
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
                    <description>Demo project for Spring Boot</description>
                    </project>""";
        assertEquals(ref, s2);
    }


    @Test
    void modifierFichier2() throws Exception {
        // ARRANGE
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
                    <description>Demo project for Spring Boot</description>
                    </project>""";
        var pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, s);
        var liste = List.of(List.of("project", "version"));
//        Position debut = new Position(13, 30, 640);
//        Position fin = new Position(13, 43, 653);
        Position debut = new Position(16, 18, 761);
        Position fin = new Position(16, 45, 788);
        String nouvelleVersion = "test simple";

        // ACT
//        xmlParserService.modifierFichier(pomFile.toString(),debut,fin,nouvelleVersion);
        xmlParserService.modifierFichier2(pomFile.toString(),debut,fin,nouvelleVersion);

        // ASSERT
        var s2 = Files.readString(pomFile);
        assertTrue(s2.contains(nouvelleVersion), () -> s2);
        var ref="""
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
                    <description>test simple</description>
                    </project>""";
        assertEquals(ref, s2);
    }
}