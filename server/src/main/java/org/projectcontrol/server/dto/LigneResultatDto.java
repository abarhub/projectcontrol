package org.projectcontrol.server.dto;

public class LigneResultatDto {

    private int noLigne;
    private String ligne;
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
}
