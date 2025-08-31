package org.projectcontrol.core.utils;

import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public class CacheCriteresRecherche {

    private final String[] texte2;
    private final List<Pattern> regexes2;
    private final List<List<String>> listeChemins;
    private final List<String> cheminXPath;
    Splitter splitter = Splitter.on(".").trimResults().omitEmptyStrings();

    public CacheCriteresRecherche(GrepParam grepParam) {
        var textes = grepParam.getCriteresRecherche().getTexte();
        var regexes = grepParam.getCriteresRecherche().getRegex();
        texte2 = textes == null ? null : textes.toArray(new String[0]);
        regexes2 = regexes == null ? null : regexes.stream()
                .map(Pattern::compile).toList();
        var champs = grepParam.getCriteresRecherche().getChamps();
        listeChemins = champs == null ? null : champs.stream()
                .map(x -> splitter.splitToList(x)).toList();
        cheminXPath = grepParam.getCriteresRecherche().getXpath();
    }

    public boolean contientTexte(String ligne) {
        return contientTexte(ligne, texte2, regexes2);
    }

    public boolean isRechercheTextuel() {
        return texte2 != null || regexes2 != null;
    }

    private boolean contientTexte(String ligne, String[] texte, List<Pattern> regexes) {
        if (StringUtils.containsAny(ligne, texte)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(regexes)) {
            for (Pattern regex : regexes) {
                if (Pattern.matches(regex.pattern(), ligne)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isRechercheChamps() {
        return listeChemins != null;
    }

    public List<List<String>> getListeChemins() {
        return listeChemins;
    }

    public boolean isRechercheXPath() {
        return cheminXPath != null;
    }

    public List<String> getCheminXPath() {
        return cheminXPath;
    }
}
