package org.projectcontrol.server.service;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
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

    private ChangementConfigService changementConfigService = new ChangementConfigService();

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

                var s = "* Parametre à ajouter : \n" +
                        "* Parametre à modifier : \n" +
                        "* Parametre à supprimer : \n";

                assertThat(res).isEqualTo(s);

            });
        }

        @Test
        void compareYamlFiles2() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version3"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.compareYamlFiles(repoDir.toString(), firstShortHash, secondShortHash,
                        "src/main/java/resources/config/application.yml");

                var s = "* Parametre à ajouter : \n" +
                        "app.key004: ffff\n" +
                        "key8: wwww\n" +
                        "* Parametre à modifier : \n" +
                        "app.key001: aaa03\n" +
                        "key2: tutu02\n" +
                        "* Parametre à supprimer : \n" +
                        "app.key003\n" +
                        "key3\n";

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
                var s = "*** Analyse de : src/main/java/resources/config/application.yml ***\n" +
                        "* Parametre à ajouter : \n" +
                        "* Parametre à modifier : \n" +
                        "* Parametre à supprimer : \n";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier yml")
        @Test
        void calculDifference2() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version3"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "*** Analyse de : src/main/java/resources/config/application.yml ***\n" +
                        "* Parametre à ajouter : \n" +
                        "app.key004: ffff\n" +
                        "key8: wwww\n" +
                        "* Parametre à modifier : \n" +
                        "app.key001: aaa03\n" +
                        "key2: tutu02\n" +
                        "* Parametre à supprimer : \n" +
                        "app.key003\n" +
                        "key3\n";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier xml")
        @Test
        void calculDifference3Xml() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version4"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "*** Analyse de : src/main/java/resources/config/application.yml ***\n" +
                        "* Parametre à ajouter : \n" +
                        "* Parametre à modifier : \n" +
                        "* Parametre à supprimer : \n" +
                        "*** Analyse de : src/main/java/resources/config/logback.xml ***\n" +
                        "@@ -0,0 +1,19 @@\n" +
                        "+<configuration>\n" +
                        "+\n" +
                        "+    <appender name=\"CONSOLE001\" class=\"ch.qos.logback.core.ConsoleAppender\">\n" +
                        "+        <layout class=\"ch.qos.logback.classic.PatternLayout\">\n" +
                        "+            <Pattern>\n" +
                        "+                %d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n\n" +
                        "+            </Pattern>\n" +
                        "+        </layout>\n" +
                        "+    </appender>\n" +
                        "+\n" +
                        "+    <logger name=\"com.test\" level=\"debug\" additivity=\"false\">\n" +
                        "+        <appender-ref ref=\"CONSOLE\"/>\n" +
                        "+    </logger>\n" +
                        "+\n" +
                        "+    <root level=\"error\">\n" +
                        "+        <appender-ref ref=\"CONSOLE001\"/>\n" +
                        "+    </root>\n" +
                        "+\n" +
                        "+</configuration>\n";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier properties")
        @Test
        void calculDifference4Properties() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version5"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "*** Analyse de : src/main/java/resources/config/application.properties ***\n" +
                        "* Parametre à ajouter : \n" +
                        "key1: aaa\n" +
                        "key2: bbbb\n" +
                        "key4: ddd\n" +
                        "key5: xxx\n" +
                        "* Parametre à modifier : \n" +
                        "* Parametre à supprimer : \n" +
                        "*** Analyse de : src/main/java/resources/config/application.yml ***\n" +
                        "* Parametre à ajouter : \n" +
                        "* Parametre à modifier : \n" +
                        "* Parametre à supprimer : \n";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour un fichier properties")
        @Test
        void calculDifference4Properties2() throws Exception {

            initialiseFichiers(repoDir, List.of("version5", "version6"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "*** Analyse de : src/main/java/resources/config/application.properties ***\n" +
                        "* Parametre à ajouter : \n" +
                        "key6: nnnn\n" +
                        "* Parametre à modifier : \n" +
                        "key2: bbbb2\n" +
                        "* Parametre à supprimer : \n" +
                        "key5\n";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec des différences pour des fichiers binaires")
        @Test
        void calculDifference5Binaire() throws Exception {

            initialiseFichiers(repoDir, List.of("version2", "version7"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "*** Analyse de : src/main/java/resources/config/test.jks ***\n" +
                        "fichier binaire ajouté\n" +
                        "*** Analyse de : src/main/java/resources/config/test.p12 ***\n" +
                        "fichier binaire ajouté\n";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec aucune différence pour des fichiers binaires")
        @Test
        void calculDifference5Binaire2() throws Exception {

            initialiseFichiers(repoDir, List.of("version7", "version2"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "";
                assertThat(res).isEqualTo(s);
            });
        }

        @DisplayName("Calcul difference entre deux versions, avec suppression d'un des fichiers binaires")
        @Test
        void calculDifference5Binaire3() throws Exception {

            initialiseFichiers(repoDir, List.of("version2", "version7"),
                    List.of(List.of(), List.of("src/main/java/resources/config/test.p12")),
                    (firstShortHash, secondShortHash) -> {

                        {// tous les commits
                            var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                            var s = "*** Analyse de : src/main/java/resources/config/test.jks ***\n" +
                                    "fichier binaire ajouté\n";
                            assertThat(res).isEqualTo(s);
                        }
                        {// les 2 derniers commits
                            var res = changementConfigService.calculDifference(repoDir, "HEAD~1", "HEAD");
                            var s = "*** Analyse de : src/main/java/resources/config/test.p12 ***\n" +
                                    "fichier binaire supprimé\n";
                            assertThat(res).isEqualTo(s);
                        }

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
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Path destinationDir = target.resolve(relative);
                Files.createDirectories(destinationDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
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