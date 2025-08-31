package org.projectcontrol.cli.utils;

import picocli.CommandLine;

import java.util.List;

public class CritereRechercheCli {

    @CommandLine.Option(names = "--texte", description = "Le texte recherché", required = false)
    private List<String> texte;

    @CommandLine.Option(names = "--regex", description = "La regex recherché", required = false)
    private List<String> regex;

    @CommandLine.Option(names = "--cheminRecherche", description = "Le chemin de recherche", required = false)
    private List<String> cheminRecherche;

    @CommandLine.Option(names = "--cheminXPath", description = "Le chemin XPath de recherche", required = false)
    private List<String> cheminXPath;

    public List<String> getTexte() {
        return texte;
    }

    public void setTexte(List<String> texte) {
        this.texte = texte;
    }

    public List<String> getRegex() {
        return regex;
    }

    public void setRegex(List<String> regex) {
        this.regex = regex;
    }

    public List<String> getCheminRecherche() {
        return cheminRecherche;
    }

    public void setCheminRecherche(List<String> cheminRecherche) {
        this.cheminRecherche = cheminRecherche;
    }

    public List<String> getCheminXPath() {
        return cheminXPath;
    }

    public void setCheminXPath(List<String> cheminXPath) {
        this.cheminXPath = cheminXPath;
    }

    @Override
    public String toString() {
        return "CritereRechercheCli{" +
                "texte=" + texte +
                ", regex=" + regex +
                ", cheminRecherche=" + cheminRecherche +
                ", cheminXPath=" + cheminXPath +
                '}';
    }
}
