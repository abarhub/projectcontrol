package org.projectcontrol.server.dto;

import java.util.List;

public class LigneResultatDto {

    private int noLigne;
    private String ligne;
    private List<String> lignes;
    private String fichier;
    private String repertoireParent;

    public int getNoLigne() {
        return noLigne;
    }

    public void setNoLigne(int noLigne) {
        this.noLigne = noLigne;
    }

    public String getLigne() {
        return ligne;
    }

    public void setLigne(String ligne) {
        this.ligne = ligne;
    }

    public String getFichier() {
        return fichier;
    }

    public void setFichier(String fichier) {
        this.fichier = fichier;
    }

    public String getRepertoireParent() {
        return repertoireParent;
    }

    public void setRepertoireParent(String repertoireParent) {
        this.repertoireParent = repertoireParent;
    }

    public List<String> getLignes() {
        return lignes;
    }

    public void setLignes(List<String> lignes) {
        this.lignes = lignes;
    }
}
