package org.projectcontrol.server.properties;

import java.util.List;

public class RunCommandeProperties {
    private String nom;
    private List<String> commande;
    private boolean shell;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public List<String> getCommande() {
        return commande;
    }

    public void setCommande(List<String> commande) {
        this.commande = commande;
    }

    public boolean isShell() {
        return shell;
    }

    public void setShell(boolean shell) {
        this.shell = shell;
    }

    @Override
    public String toString() {
        return "RunCommandeProperties{" +
                "nom='" + nom + '\'' +
                ", commande=" + commande +
                ", shell=" + shell +
                '}';
    }
}
