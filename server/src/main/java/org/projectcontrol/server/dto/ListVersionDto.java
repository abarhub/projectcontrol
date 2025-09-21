package org.projectcontrol.server.dto;

import java.util.List;

public class ListVersionDto {

    private String versionActuelle;
    private List<String> listeVersions;
    private String messageCommit;
    private List<FichierAModifieDto> fichierAModifier;
    private String id;

    public String getVersionActuelle() {
        return versionActuelle;
    }

    public void setVersionActuelle(String versionActuelle) {
        this.versionActuelle = versionActuelle;
    }

    public List<String> getListeVersions() {
        return listeVersions;
    }

    public void setListeVersions(List<String> listeVersions) {
        this.listeVersions = listeVersions;
    }

    public String getMessageCommit() {
        return messageCommit;
    }

    public void setMessageCommit(String messageCommit) {
        this.messageCommit = messageCommit;
    }

    public List<FichierAModifieDto> getFichierAModifier() {
        return fichierAModifier;
    }

    public void setFichierAModifier(List<FichierAModifieDto> fichierAModifier) {
        this.fichierAModifier = fichierAModifier;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ListVersionDto{" +
                "versionActuelle='" + versionActuelle + '\'' +
                ", listeVersions=" + listeVersions +
                ", messageCommit='" + messageCommit + '\'' +
                ", fichierAModifier=" + fichierAModifier +
                ", id='" + id + '\'' +
                '}';
    }
}
