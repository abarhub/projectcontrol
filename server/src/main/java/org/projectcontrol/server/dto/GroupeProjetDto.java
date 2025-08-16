package org.projectcontrol.server.dto;

import java.util.Map;

public class GroupeProjetDto {

    private Map<String, String> groupeId;
    private String groupeIdDefaut;

    public Map<String, String> getGroupeId() {
        return groupeId;
    }

    public void setGroupeId(Map<String, String> groupeId) {
        this.groupeId = groupeId;
    }

    public String getGroupeIdDefaut() {
        return groupeIdDefaut;
    }

    public void setGroupeIdDefaut(String groupeIdDefaut) {
        this.groupeIdDefaut = groupeIdDefaut;
    }
}
