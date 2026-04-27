package org.projectcontrol.server.service;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangementConfigServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangementConfigServiceTest.class);

    private final ChangementConfigService changementConfigService = new ChangementConfigService();

    private static final Path REPERTOIRE_REFERENCE = Paths.get("src/test/resources/changeConfig")
            .toAbsolutePath().normalize();

    private Path repoDir;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        repoDir = tempDir.resolve("repo_test");
        Files.createDirectories(repoDir);
    }

    @Nested
    class TestCompareYamlFiles {

        @Test
        void compareYamlFiles() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version2"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.compareYamlFiles(repoDir.toString(), firstShortHash, secondShortHash,
                        "src/main/java/resources/config/application.yml");

                var s = """
                        * Parametre à ajouter :\s
                        * Parametre à modifier :\s
                        * Parametre à supprimer :\s
                        """;

                assertThat(res).isEqualTo(s);

            });
        }

        @Test
        void compareYamlFiles2Modification() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version3"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.compareYamlFiles(repoDir.toString(), firstShortHash, secondShortHash,
                        "src/main/java/resources/config/application.yml");

                var s = """
                        * Parametre à ajouter :\s
                        app.key004: ffff
                        key8: wwww
                        * Parametre à modifier :\s
                        app.key001: aaa03
                        key2: tutu02
                        * Parametre à supprimer :\s
                        app.key003
                        key3
                        """;

                assertThat(res).isEqualTo(s);

            });
        }

        @Test
        void compareYamlFiles3Suppression() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version3", "version2"),
                    List.of(List.of(), List.of(), List.of("src/main/java/resources/config/application.yml")),
                    (firstShortHash, secondShortHash) -> {
                        var res = changementConfigService.compareYamlFiles(repoDir.toString(), firstShortHash, secondShortHash,
                                "src/main/java/resources/config/application.yml");

                        var s = """
                                * Parametre à ajouter :\s
                                * Parametre à modifier :\s
                                * Parametre à supprimer :\s
                                app.key001
                                app.key002
                                app.key003
                                key1
                                key2
                                key3
                                """;

                        assertThat(res).isEqualTo(s);

                    });
        }

    }

    @Nested
    class TestCalculDifference {

        @DisplayName("Calcul difference entre deux versions, avec aucune différence")
        @Test
        void calculDifference() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version2"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/application.yml ***
                        * Parametre à ajouter :\s
                        * Parametre à modifier :\s
                        * Parametre à supprimer :\s
                        """;
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier yml")
        @Test
        void calculDifference2() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version3"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/application.yml ***
                        * Parametre à ajouter :\s
                        app.key004: ffff
                        key8: wwww
                        * Parametre à modifier :\s
                        app.key001: aaa03
                        key2: tutu02
                        * Parametre à supprimer :\s
                        app.key003
                        key3
                        """;
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier xml")
        @Test
        void calculDifference3Xml() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version4"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/application.yml ***
                        * Parametre à ajouter :\s
                        * Parametre à modifier :\s
                        * Parametre à supprimer :\s
                        *** Analyse de : src/main/java/resources/config/logback.xml ***
                        @@ -0,0 +1,19 @@
                        +<configuration>
                        +
                        +    <appender name="CONSOLE001" class="ch.qos.logback.core.ConsoleAppender">
                        +        <layout class="ch.qos.logback.classic.PatternLayout">
                        +            <Pattern>
                        +                %d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
                        +            </Pattern>
                        +        </layout>
                        +    </appender>
                        +
                        +    <logger name="com.test" level="debug" additivity="false">
                        +        <appender-ref ref="CONSOLE"/>
                        +    </logger>
                        +
                        +    <root level="error">
                        +        <appender-ref ref="CONSOLE001"/>
                        +    </root>
                        +
                        +</configuration>
                        """;
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier properties")
        @Test
        void calculDifference4Properties() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version5"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/application.properties ***
                        * Parametre à ajouter :\s
                        key1: aaa
                        key2: bbbb
                        key4: ddd
                        key5: xxx
                        * Parametre à modifier :\s
                        * Parametre à supprimer :\s
                        *** Analyse de : src/main/java/resources/config/application.yml ***
                        * Parametre à ajouter :\s
                        * Parametre à modifier :\s
                        * Parametre à supprimer :\s
                        """;
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier properties")
        @Test
        void calculDifference4Properties2() throws Exception {

            initialiseFichiers(repoDir, List.of("version5", "version6"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/application.properties ***
                        * Parametre à ajouter :\s
                        key6: nnnn
                        * Parametre à modifier :\s
                        key2: bbbb2
                        * Parametre à supprimer :\s
                        key5
                        """;
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour des fichiers binaires")
        @Test
        void calculDifference5Binaire() throws Exception {

            initialiseFichiers(repoDir, List.of("version2", "version7"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/test.jks ***
                        fichier binaire ajouté
                        *** Analyse de : src/main/java/resources/config/test.p12 ***
                        fichier binaire ajouté
                        """;
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec aucune différence pour des fichiers binaires")
        @Test
        void calculDifference6Binaire2() throws Exception {

            initialiseFichiers(repoDir, List.of("version7", "version2"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec suppression d'un des fichiers binaires")
        @Test
        void calculDifference7Binaire3() throws Exception {

            initialiseFichiers(repoDir, List.of("version2", "version7"),
                    List.of(List.of(), List.of("src/main/java/resources/config/test.p12")),
                    (firstShortHash, secondShortHash) -> {

                        {// tous les commits
                            var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                            var s = """
                                    *** Analyse de : src/main/java/resources/config/test.jks ***
                                    fichier binaire ajouté
                                    """;
                            assertThat(res).isEqualTo(s);
                        }
                        {// les 2 derniers commits
                            var res = changementConfigService.calculDifference(repoDir, "HEAD~1", "HEAD");
                            var s = """
                                    *** Analyse de : src/main/java/resources/config/test.p12 ***
                                    fichier binaire supprimé
                                    """;
                            assertThat(res).isEqualTo(s);
                        }

                    });
        }

        @DisplayName("Calcul difference entre deux versions, avec des fichiers binaires et texte d'un un pdf")
        @Test
        void calculDifference8Binaire4() throws Exception {

            initialiseFichiers(repoDir, List.of("version2", "version9"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = """
                        *** Analyse de : src/main/java/resources/config/test1.txt ***
                        @@ -0,0 +1,10 @@
                        +abc c'est un test
                        +hello world !
                        +
                        +aaaa
                        +
                        +bbbb
                        +
                        +
                        +cccccccccccccccccccccccccccccccccccc
                        +
                        *** Analyse de : src/main/java/resources/config/test2.pdf ***
                        fichier binaire ajouté
                        """;
                assertThat(res).isEqualTo(s);
            });
        }
    }

    @Nested
    class TestFindConfigfiles {

        @Test
        void findConfigFiles() throws Exception {


            initialiseFichiers(repoDir, List.of("version1"), (firstShortHash, secondShortHash) -> {

                var listeConfigfiles = changementConfigService.findConfigFiles(repoDir.toString(), firstShortHash, secondShortHash);

                LOGGER.info("repoDir={}", repoDir);
                LOGGER.info("listeConfigfiles={}", listeConfigfiles);

                assertThat(listeConfigfiles)
                        .hasSize(1);
                assertThat(listeConfigfiles.getFirst().replace('\\', '/'))
                        .isEqualTo("src/main/java/resources/config/application.yml");
            });

        }

        @Test
        void findConfigFiles2() throws Exception {


            initialiseFichiers(repoDir, List.of("version1", "version4", "version7", "version8"), (firstShortHash, secondShortHash) -> {

                var listeConfigfiles = changementConfigService.findConfigFiles(repoDir.toString(), firstShortHash, secondShortHash);

                LOGGER.info("repoDir={}", repoDir);
                LOGGER.info("listeConfigfiles={}", listeConfigfiles);


                assertThat(listeConfigfiles)
                        .hasSize(7) // Vérifie la taille de la liste
                        .extracting(path -> path.replace('\\', '/')) // Transformation
                        .containsExactlyInAnyOrder(
                                "src/main/java/resources/config/application-config.yml",
                                "src/main/java/resources/config/application.properties",
                                "src/main/java/resources/config/application.yml",
                                "src/main/java/resources/config/donnees/donnees.json",
                                "src/main/java/resources/config/logback.xml",
                                "src/main/java/resources/config/test.jks",
                                "src/main/java/resources/config/test.p12"
                        );
            });

        }

    }

    private void initialiseFichiers(Path repoDir, List<String> listeRepertoires,
                                    FailableBiConsumer<String, String, Exception> consumer) throws Exception {
        initialiseFichiers(repoDir, listeRepertoires, List.of(), consumer);
    }

    private void initialiseFichiers(Path repoDir, List<String> listeRepertoires, List<List<String>> fichiersASupprimer,
                                    FailableBiConsumer<String, String, Exception> consumer) throws Exception {
        try (Git git = Git.init()
                .setDirectory(repoDir.toFile())
                .call()) {

            String premierCommit = null;
            String dernierCommit = null;
            int no = 0;

            for (String repertoire : listeRepertoires) {

                Path rep = REPERTOIRE_REFERENCE.resolve(repertoire);

                copyDirectory(rep, repoDir);

                git.add()
                        .addFilepattern(".")
                        .call();

                RevCommit firstCommit = git.commit()
                        .setMessage("commit")
                        .call();

                if (premierCommit == null) {
                    premierCommit = shortHash(firstCommit.getId());
                }
                dernierCommit = shortHash(firstCommit.getId());

                boolean modification = false;

                if (no >= 0 && fichiersASupprimer.size() > no) {
                    var listeFichiersASupprimer = fichiersASupprimer.get(no);
                    if (listeFichiersASupprimer != null) {
                        for (String fichier : listeFichiersASupprimer) {
                            var path = repoDir.resolve(fichier).toAbsolutePath().normalize();
                            LOGGER.info("Suppression du fichier {}", fichier);
                            assertTrue(path.startsWith(repoDir));
                            if (Files.exists(path)) {
                                Files.delete(path);
                                modification = true;
                            }
                        }
                    }
                }

                if (modification) {
                    git.add()
                            .addFilepattern(".")
                            .call();

                    RevCommit commit = git.commit()
                            .setMessage("commit delete")
                            .call();

                    dernierCommit = shortHash(commit.getId());
                }

                no++;
            }

            LOGGER.info("First commit:  {}", premierCommit);
            LOGGER.info("Second commit: {}", dernierCommit);

            consumer.accept(premierCommit, dernierCommit);


        }
    }

    private static String shortHash(ObjectId objectId) {
        return objectId.abbreviate(7).name();
    }

    /**
     * Copie récursive d'un dossier source vers destination
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(@NonNull Path dir,
                                                     @NonNull BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Path destinationDir = target.resolve(relative);
                Files.createDirectories(destinationDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(@NonNull Path file,
                                             @NonNull BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path destinationFile = target.resolve(relative);

                Files.copy(
                        file,
                        destinationFile,
                        StandardCopyOption.REPLACE_EXISTING
                );

                return FileVisitResult.CONTINUE;
            }
        });
    }
}