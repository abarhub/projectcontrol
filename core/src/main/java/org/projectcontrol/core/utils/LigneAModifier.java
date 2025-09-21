package org.projectcontrol.core.utils;

import java.util.List;

public record LigneAModifier(String nomFichier, int noLigne, List<Interval> positionModification) {
}
