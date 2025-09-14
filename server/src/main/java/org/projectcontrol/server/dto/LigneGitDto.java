package org.projectcontrol.server.dto;

public class LigneGitDto {
    private String ligne;
    private boolean commentaire;

    public String getLigne() {
        return ligne;
    }

    public void setLigne(String ligne) {
        this.ligne = ligne;
    }

    public boolean isCommentaire() {
        return commentaire;
    }

    public void setCommentaire(boolean commentaire) {
        this.commentaire = commentaire;
    }
}
