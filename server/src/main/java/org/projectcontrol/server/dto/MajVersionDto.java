package org.projectcontrol.server.dto;

import java.util.List;

public class MajVersionDto {

    private String version;
    private String messageCommit;
    private boolean commit;
    private String id;
    private List<String> listeIdLignes;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessageCommit() {
        return messageCommit;
    }

    public void setMessageCommit(String messageCommit) {
        this.messageCommit = messageCommit;
    }

    public boolean isCommit() {
        return commit;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getListeIdLignes() {
        return listeIdLignes;
    }

    public void setListeIdLignes(List<String> listeIdLignes) {
        this.listeIdLignes = listeIdLignes;
    }

    @Override
    public String toString() {
        return "MajVersionDto{" +
                "version='" + version + '\'' +
                ", messageCommit='" + messageCommit + '\'' +
                ", commit=" + commit +
                ", id='" + id + '\'' +
                ", listeIdLignes=" + listeIdLignes +
                '}';
    }
}
