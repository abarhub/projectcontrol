package org.projectcontrol.core.utils;

import java.util.List;

public class GrepParam {

    private GrepCriteresRecherche criteresRecherche;
    private List<String> repertoires;
    private List<String> exclusions;
    private List<String> extensionsFichiers;
    private int nbLignesAutour;

    public GrepCriteresRecherche getCriteresRecherche() {
        return criteresRecherche;
    }

    public void setCriteresRecherche(GrepCriteresRecherche criteresRecherche) {
        this.criteresRecherche = criteresRecherche;
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

    @Override
    public String toString() {
        return "GrepParam{" +
                "criteresRecherche=" + criteresRecherche +
                ", repertoires=" + repertoires +
                ", exclusions=" + exclusions +
                ", extensionsFichiers=" + extensionsFichiers +
                ", nbLignesAutour=" + nbLignesAutour +
                '}';
    }
}
