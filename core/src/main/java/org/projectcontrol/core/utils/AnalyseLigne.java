package org.projectcontrol.core.utils;

public class AnalyseLigne {
    private int noLigne;
    private String ligne;
    private EtatLigneEnum etatLigne;

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

    public EtatLigneEnum getEtatLigne() {
        return etatLigne;
    }

    public void setEtatLigne(EtatLigneEnum etatLigne) {
        this.etatLigne = etatLigne;
    }
}
