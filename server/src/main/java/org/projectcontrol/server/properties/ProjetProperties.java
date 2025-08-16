package org.projectcontrol.server.properties;

import java.util.List;

public class ProjetProperties {

    private String nom;
    private List<String> repertoires;
    private List<String> exclusions;
    private boolean defaut;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
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

    public boolean isDefaut() {
        return defaut;
    }

    public void setDefaut(boolean defaut) {
        this.defaut = defaut;
    }
}
