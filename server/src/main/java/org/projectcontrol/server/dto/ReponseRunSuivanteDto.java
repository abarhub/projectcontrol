package org.projectcontrol.server.dto;

import java.util.List;

public class ReponseRunSuivanteDto {

    private String id;
    private List<String> listeLignes;
    private boolean terminer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getListeLignes() {
        return listeLignes;
    }

    public void setListeLignes(List<String> listeLignes) {
        this.listeLignes = listeLignes;
    }

    public boolean isTerminer() {
        return terminer;
    }

    public void setTerminer(boolean terminer) {
        this.terminer = terminer;
    }
}
