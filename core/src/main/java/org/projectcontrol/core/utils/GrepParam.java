package org.projectcontrol.core.utils;

import java.util.List;

public class GrepParam {


    private String texte;
    private List<String> repertoires;
    private List<String> exclusions;
    private List<String> extensionsFichiers;
    private int nbLignesAutour;

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public List<String> getRepertoires() {
        return repertoires;
    }

    public void setRepertoires(List<String> repertoires) {
        this.repertoires = repertoires;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }

    public List<String> getExtensionsFichiers() {
        return extensionsFichiers;
    }

    public void setExtensionsFichiers(List<String> extensionsFichiers) {
        this.extensionsFichiers = extensionsFichiers;
    }

    public int getNbLignesAutour() {
        return nbLignesAutour;
    }

    public void setNbLignesAutour(int nbLignesAutour) {
        this.nbLignesAutour = nbLignesAutour;
    }
}
