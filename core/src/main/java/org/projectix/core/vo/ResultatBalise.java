package org.projectix.core.vo;

import java.util.List;

public record ResultatBalise(List<String> balises,String valeur, Position positionDebut, Position positionFin) {
}
