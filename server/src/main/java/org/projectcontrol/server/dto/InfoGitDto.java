package org.projectcontrol.server.dto;

import java.time.LocalDateTime;
import java.util.List;

public class InfoGitDto {

    private LocalDateTime date;
    private String idCommit;
    private String idCommitComplet;
    private String branche;
    private String message;
    private List<String> fichiersNonCommite;
    private List<String> fichiersNonTracke;
    private List<String> listeBranchesCommit;

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getIdCommit() {
        return idCommit;
    }

    public void setIdCommit(String idCommit) {
        this.idCommit = idCommit;
    }

    public String getIdCommitComplet() {
        return idCommitComplet;
    }

    public void setIdCommitComplet(String idCommitComplet) {
        this.idCommitComplet = idCommitComplet;
    }

    public String getBranche() {
        return branche;
    }

    public void setBranche(String branche) {
        this.branche = branche;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getFichiersNonCommite() {
        return fichiersNonCommite;
    }

    public void setFichiersNonCommite(List<String> fichiersNonCommite) {
        this.fichiersNonCommite = fichiersNonCommite;
    }

    public List<String> getFichiersNonTracke() {
        return fichiersNonTracke;
    }

    public void setFichiersNonTracke(List<String> fichiersNonTracke) {
        this.fichiersNonTracke = fichiersNonTracke;
    }

    public List<String> getListeBranchesCommit() {
        return listeBranchesCommit;
    }

    public void setListeBranchesCommit(List<String> listeBranchesCommit) {
        this.listeBranchesCommit = listeBranchesCommit;
    }
}
