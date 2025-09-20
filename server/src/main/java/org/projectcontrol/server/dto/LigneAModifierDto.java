package org.projectcontrol.server.dto;

import org.projectcontrol.core.utils.Interval;

import java.util.List;

public record LigneAModifierDto(int ligne,String contenu,boolean trouve, List<Interval> positionModification) {
}
