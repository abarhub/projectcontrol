package org.projectcontrol.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.projectcontrol.core.utils.GrepCriteresRecherche;
import org.projectcontrol.core.utils.GrepParam;
import org.projectcontrol.core.utils.LignesRecherche;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GrepServiceTest {

    private GrepService grepService = new GrepService();

    @TempDir
    private Path repertoire;

    private Path rep1;

    private GrepParam grepParam = new GrepParam();

    @BeforeEach
    void setUp() throws Exception {
        rep1 = repertoire.resolve("rep1");
        Files.createDirectory(rep1);
        initRepertoire(rep1);

        grepParam = new GrepParam();
        grepParam.setRepertoires(List.of(rep1.toString()));
        grepParam.setExclusions(GrepService.REPERTOIRES_EXCLUSION);
        grepParam.setExtensionsFichiers(GrepService.EXTENSIONS_FICHIERS_DEFAULT);
    }

    @Nested
    class SearchTextuel {

        @Test
        void search() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setTexte(List.of("simple"));
            grepParam.setCriteresRecherche(criteresRecherche);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(2)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(2, List.of(2), List.of("très simple a rechercher"), "file1.txt"),
                            tuple(2, List.of(2), List.of("très simple a rechercher ici2"), "dir1/file3.java"));
        }

        @Test
        void search2() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setTexte(List.of("exemple"));
            grepParam.setCriteresRecherche(criteresRecherche);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(3)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(1, List.of(1), List.of("un exemple de fichier"), "file1.txt"),
                            tuple(1, List.of(1), List.of("un autre exemple de fichier à chercher2"), "dir1/file3.java"),
                            tuple(4, List.of(4), List.of("exemple2"), "dir1/file3.java"));
        }

        @Test
        void search3() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setTexte(List.of("instant2"));
            grepParam.setCriteresRecherche(criteresRecherche);
            grepParam.setExtensionsFichiers(List.of("tmp"));

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(3, List.of(3), List.of("fin du texte pour l'instant2"), "dir1/file4.tmp"));
        }

    }

    @Nested
    class SearchJson {

        @Test
        void search() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champs2"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    {
                      "champ1": 1,
                      "champs2": "xxx",
                      "champ3": {
                        "champs4": "ZZZZZZZZZZ",
                        "champs5": "ZZZZZZZZZZ",
                        "champs6": "ZZZZZZZZZZ"
                      },
                      "champs7": [
                        "aaa","bbb", "ccc"
                      ]
                    }""";

            Files.writeString(rep1.resolve("file5.json"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champs2=\"xxx\""), "file5.json"));
        }

        @Test
        void search2() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champ3.champs6"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    {
                      "champ1": 1,
                      "champs2": "xxx",
                      "champ3": {
                        "champs4": "AAAA",
                        "champs5": "BBBB",
                        "champs6": "CCCC"
                      },
                      "champs7": [
                        "aaa","bbb", "ccc"
                      ]
                    }""";

            Files.writeString(rep1.resolve("file5.json"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champ3.champs6=\"CCCC\""), "file5.json"));
        }

        @Test
        void search3() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champs7"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    {
                      "champ1": 1,
                      "champs2": "xxx",
                      "champ3": {
                        "champs4": "AAAA",
                        "champs5": "BBBB",
                        "champs6": "CCCC"
                      },
                      "champs7": [
                        "aaa","bbb", "ccc"
                      ]
                    }""";

            Files.writeString(rep1.resolve("file5.json"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champs7=[\"aaa\",\"bbb\",\"ccc\"]"), "file5.json"));
        }


        @Test
        void search4() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champ3"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    {
                      "champ1": 1,
                      "champs2": "xxx",
                      "champ3": {
                        "champs4": "AAAA",
                        "champs5": "BBBB",
                        "champs6": "CCCC"
                      },
                      "champs7": [
                        "aaa","bbb", "ccc"
                      ]
                    }""";

            Files.writeString(rep1.resolve("file5.json"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champ3={\"champs4\":\"AAAA\",\"champs5\":\"BBBB\",\"champs6\":\"CCCC\"}"), "file5.json"));
        }

    }

    @Nested
    class SearchYml {

        @Test
        void search() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champ1"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    champ1: value1
                    champ2: value2
                    champ3:
                      champ31: value31
                      champ32: value32
                      champ33: value33
                    champ4: value4
                    champ5: value5
                    champ6:
                      champ61: value61
                      champ62: value62
                      champ63: [ value631, value632]""";

            Files.writeString(rep1.resolve("file6.yml"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champ1: value1"), "file6.yml"));
        }

        @Test
        void search2() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champs3.champ31"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    champ1: value1
                    champ2: value2
                    champs3:
                      champ31: value31
                      champ32: value32
                      champ33: value33
                    champ4: value4
                    champ5: value5
                    champ6:
                      champ61: value61
                      champ62: value62
                      champ63: [ value631, value632]""";

            Files.writeString(rep1.resolve("file6.yml"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champs3.champ31: value31"), "file6.yml"));
        }

        @Test
        void search3() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champ6.champ63"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    champ1: value1
                    champ2: value2
                    champs3:
                      champ31: value31
                      champ32: value32
                      champ33: value33
                    champ4: value4
                    champ5: value5
                    champ6:
                      champ61: value61
                      champ62: value62
                      champ63: [ value631, value632]""";

            Files.writeString(rep1.resolve("file6.yml"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champ6.champ63: [value631, value632]"), "file6.yml"));
        }

        @Test
        void search4() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setChamps(List.of("champ6"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    champ1: value1
                    champ2: value2
                    champs3:
                      champ31: value31
                      champ32: value32
                      champ33: value33
                    champ4: value4
                    champ5: value5
                    champ6:
                      champ61: value61
                      champ62: value62
                      champ63: [ value631, value632]""";

            Files.writeString(rep1.resolve("file6.yml"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("champ6: {champ61=value61, champ62=value62, champ63=[value631, value632]}"), "file6.yml"));
        }

    }


    @Nested
    class SearchXml {

        @Test
        void search() throws Exception {
            // ARRANGE
            GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
            criteresRecherche.setXpath(List.of("/project/version"));
            grepParam.setCriteresRecherche(criteresRecherche);

            String s1 = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>

                        <parent>
                            <groupId>org.projectcontrol</groupId>
                            <artifactId>projectcontrol-parent</artifactId>
                            <version>0.0.1-SNAPSHOT</version>
                        </parent>

                        <artifactId>core</artifactId>
                        <version>0.0.2-SNAPSHOT</version>

                        <dependencies>

                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </project>""";

            Files.writeString(rep1.resolve("file7.xml"), s1);

            // ACT
            var res = grepService.search(grepParam);

            // ASSERT
            assertNotNull(res);
            var liste = res.blockingIterable();
            assertNotNull(liste);
            assertThat(liste)
                    .hasSize(1)
                    .extracting(LignesRecherche::noLigneDebut, LignesRecherche::lignesTrouvees,
                            LignesRecherche::lignes, (x) -> getChemin(rep1, x))
                    .contains(tuple(0, List.of(0), List.of("/project/version: 0.0.2-SNAPSHOT"), "file7.xml"));
        }
    }

    // méthodes utilitaires

    private String getChemin(Path rep, LignesRecherche lignesRecherche) {
        if (lignesRecherche == null || lignesRecherche.ficher() == null) {
            return "";
        } else {
            Path p = rep.relativize(lignesRecherche.ficher());
            return p.toString().replaceAll("\\\\", "/");
        }
    }

    private void initRepertoire(Path rep) throws IOException {

        String s1 = """
                un exemple de fichier
                très simple a rechercher
                fin du texte""";
        String s2 = """
                un autre exemple de fichier à chercher2
                très simple a rechercher ici2
                fin du texte pour l'instant2
                exemple2""";

        Files.writeString(rep.resolve("file1.txt"), s1);
        Files.writeString(rep.resolve("file2.bin"), s1);
        Path dir1 = rep.resolve("dir1");
        Files.createDirectory(dir1);
        Files.writeString(dir1.resolve("file3.java"), s2);
        Files.writeString(dir1.resolve("file4.tmp"), s2);
    }
}