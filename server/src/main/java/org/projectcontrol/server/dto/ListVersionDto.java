package org.projectcontrol.server.dto;

import java.util.List;

public class ListVersionDto {

    private String versionActuelle;
    private List<String> listeVersions;
    private String messageCommit;

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
}
