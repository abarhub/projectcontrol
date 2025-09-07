package org.projectcontrol.core.utils;

import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public List<Interval> getPositionsTexte(String contenu) {
        List<Interval> liste = new ArrayList<>();
        if (StringUtils.containsAny(contenu, texte2)) {
            for (var s : texte2) {
                var index = contenu.indexOf(s);
                if (index != -1) {
                    liste.add(new Interval(index, index + s.length() - 1));
                    index += s.length();
                    while (index < contenu.length()) {
                        index = contenu.indexOf(s, index);
                        if (index != -1) {
                            liste.add(new Interval(index, index + s.length() - 1));
                        } else {
                            break;
                        }
                        index += s.length();
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(regexes2)) {
            for (Pattern regex : regexes2) {
                var matcher = Pattern.compile(regex.pattern()).matcher(contenu);
                while (matcher.find()) {
                    liste.add(new Interval(matcher.start(), matcher.end()));
                }
            }
        }
        if (!liste.isEmpty()) {
            liste = liste.stream().sorted(Comparator.comparing(Interval::debut))
                    .collect(Collectors.toList());
            for (int i = 0; i < liste.size() - 1; i++) {
                if (i > 0) {
                    var precedent = liste.get(i - 1);
                    var actuel = liste.get(i);
                    if (precedent.fin() >= actuel.debut() || precedent.fin() + 1 == actuel.debut()) {
                        Interval interval = new Interval(precedent.debut(), Math.max(actuel.fin(), precedent.fin()));
                        liste.set(i - 1, interval);
                        liste.remove(i);
                        i--;
                    }
                }
            }
            return liste;
        } else {
            return null;
        }
    }
}
