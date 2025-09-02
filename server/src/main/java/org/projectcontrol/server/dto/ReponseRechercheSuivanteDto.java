package org.projectcontrol.server.dto;

import java.util.List;

public class ReponseRechercheSuivanteDto {

    private String id;
    private List<LigneResultatDto> listeLignes;
    private boolean terminer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<LigneResultatDto> getListeLignes() {
        return listeLignes;
    }

    public void setListeLignes(List<LigneResultatDto> listeLignes) {
        this.listeLignes = listeLignes;
    }

    public boolean isTerminer() {
        return terminer;
    }

    public void setTerminer(boolean terminer) {
        this.terminer = terminer;
    }
}
