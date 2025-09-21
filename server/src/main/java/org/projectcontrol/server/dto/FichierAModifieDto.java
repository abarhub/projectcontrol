package org.projectcontrol.server.dto;

import java.util.*;

public class FichierAModifieDto {
    private String nomFichier;
    private String hash;
    private List<LigneAModifierDto> lignes = new ArrayList<>();

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public List<LigneAModifierDto> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneAModifierDto> lignes) {
        this.lignes = lignes;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "FichierAModifieDto{" +
                "nomFichier='" + nomFichier + '\'' +
                ", hash='" + hash + '\'' +
                ", lignes=" + lignes +
                '}';
    }
}
