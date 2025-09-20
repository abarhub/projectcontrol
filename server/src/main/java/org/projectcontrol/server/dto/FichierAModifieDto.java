package org.projectcontrol.server.dto;

import java.util.*;

public class FichierAModifieDto {
    private String nomFichier;
    private String hash;
    private Map<Integer, LigneAModifierDto> lignes = new TreeMap<>();

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public Map<Integer, LigneAModifierDto> getLignes() {
        return lignes;
    }

    public void setLignes(Map<Integer, LigneAModifierDto> lignes) {
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
