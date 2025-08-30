package org.projectcontrol.core.utils;

import java.nio.file.Path;
import java.util.List;

public record LignesRecherche(int noLigneDebut, List<String> lignes, Path ficher, List<Integer> lignesTrouvees) {
}
