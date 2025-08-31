package org.projectcontrol.server.dto;

public class LigneResultatDto {

    private int noLigne;
    private String ligne;
    private String fichier;

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
}
