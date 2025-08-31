package org.projectcontrol.core.utils;

import java.util.List;

public class GrepCriteresRecherche {

    private List<String> texte;
    private List<String> regex;
    private List<String> champs;
    private List<String> xpath;

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

    public List<String> getChamps() {
        return champs;
    }

    public void setChamps(List<String> champs) {
        this.champs = champs;
    }

    public List<String> getXpath() {
        return xpath;
    }

    public void setXpath(List<String> xpath) {
        this.xpath = xpath;
    }
}
