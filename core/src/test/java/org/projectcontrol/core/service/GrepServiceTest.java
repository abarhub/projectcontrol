package org.projectcontrol.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    @Test
    void search() throws Exception {
        // ARRANGE
        grepParam.setTexte("simple");

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
        grepParam.setTexte("exemple");

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
        grepParam.setTexte("instant2");
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