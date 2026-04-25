package org.projectcontrol.server.service;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
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

    }

    @Nested
    class TestCalculDifference {

        @Test
        void calculDifference() throws Exception {

            initialiseFichiers(repoDir, List.of("version1", "version2"), (firstShortHash, secondShortHash) -> {
                var res = changementConfigService.calculDifference(repoDir, firstShortHash, secondShortHash);
                var s = "*** Analyse de : src/main/java/resources/config/application.yml ***\n" +
                        "????\n";
                assertThat(res).isEqualTo(s);
            });
        }
    }

    @Nested
    class TestFindConfigfiles {

        @Test
        void findConfigFiles() throws Exception {

            Path firstVersionSource = REPERTOIRE_REFERENCE.resolve("version1");

            copyDirectory(firstVersionSource, repoDir);

            var listeConfigfiles = changementConfigService.findConfigFiles(repoDir.toString());

            LOGGER.info("repoDir={}", repoDir);
            LOGGER.info("listeConfigfiles={}", listeConfigfiles);
            LOGGER.info("listeConfigfiles={}", repoDir.relativize(listeConfigfiles.getFirst()));

            assertThat(listeConfigfiles)
                    .hasSize(1);
            assertThat(repoDir.relativize(listeConfigfiles.getFirst()).toString().replace('\\', '/'))
                    .isEqualTo("src/main/java/resources/config/application.yml");
        }

    }


    @Test
    void shouldCreateRepoAndCommitTwice() throws Exception {
        // Répertoires source contenant les fichiers à copier
        Path firstVersionSource = Paths.get("src/test/resources/changeConfig/version1");
        Path secondVersionSource = Paths.get("src/test/resources/changeConfig/version2");

        try (Git git = Git.init()
                .setDirectory(repoDir.toFile())
                .call()) {

            /*
             * -------------------------
             * 1er commit
             * -------------------------
             */

            copyDirectory(firstVersionSource, repoDir);

            git.add()
                    .addFilepattern(".")
                    .call();

            RevCommit firstCommit = git.commit()
                    .setMessage("Initial commit")
                    .call();

            /*
             * -------------------------
             * 2ème commit
             * -------------------------
             */

            // Ici version2 contient par exemple :
            // config/application.yml modifié
            // + éventuellement d'autres fichiers
            copyDirectory(secondVersionSource, repoDir);

            git.add()
                    .addFilepattern(".")
                    .call();

            RevCommit secondCommit = git.commit()
                    .setMessage("Update application config")
                    .call();

            /*
             * -------------------------
             * Affichage des hashes courts
             * -------------------------
             */

            String firstShortHash = shortHash(firstCommit.getId());
            String secondShortHash = shortHash(secondCommit.getId());

            LOGGER.info("First commit:  {}", firstShortHash);
            LOGGER.info("Second commit: {}", secondShortHash);

            var listeConfigfiles = changementConfigService.findConfigFiles(repoDir.toString());

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

                if (no > 0 && fichiersASupprimer.size() > no) {
                    var listeFichiersASupprimer = fichiersASupprimer.get(no);
                    if (listeFichiersASupprimer != null) {
                        for (String fichier : listeFichiersASupprimer) {
                            var path = repoDir.resolve(fichier).toAbsolutePath().normalize();
                            LOGGER.info("Suppression du fichier {}", fichier);
                            assertTrue(path.startsWith(repoDir));
                            if (Files.exists(path)) {
                                Files.delete(path);
                            }
                        }
                    }
                }

                no++;
            }

            LOGGER.info("First commit:  {}", premierCommit);
            LOGGER.info("Second commit: {}", dernierCommit);

            consumer.accept(premierCommit, dernierCommit);

//            /*
//             * -------------------------
//             * 1er commit
//             * -------------------------
//             */
//
//            copyDirectory(firstVersionSource, repoDir);
//
//            git.add()
//                    .addFilepattern(".")
//                    .call();
//
//            RevCommit firstCommit = git.commit()
//                    .setMessage("Initial commit")
//                    .call();
//
//            /*
//             * -------------------------
//             * 2ème commit
//             * -------------------------
//             */
//
//            // Ici version2 contient par exemple :
//            // config/application.yml modifié
//            // + éventuellement d'autres fichiers
//            copyDirectory(secondVersionSource, repoDir);
//
//            git.add()
//                    .addFilepattern(".")
//                    .call();
//
//            RevCommit secondCommit = git.commit()
//                    .setMessage("Update application config")
//                    .call();
//
//            /*
//             * -------------------------
//             * Affichage des hashes courts
//             * -------------------------
//             */
//
//            String firstShortHash = shortHash(firstCommit.getId());
//            String secondShortHash = shortHash(secondCommit.getId());
//
//            LOGGER.info("First commit:  {}", firstShortHash);
//            LOGGER.info("Second commit: {}", secondShortHash);
//
//            var listeConfigfiles = changementConfigService.findConfigFiles(repoDir.toString());

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