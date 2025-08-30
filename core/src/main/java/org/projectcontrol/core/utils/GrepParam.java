package org.projectcontrol.core.utils;

import java.util.List;

public class GrepParam {

    public static final List<String> REPERTOIRES_EXCLUSION = List.of("node_modules", "target", ".git");

    public static final List<String> EXTENSIONS_FICHIERS_DEFAULT = List.of("java", "ts", "xml", "html",
            "css", "scss", "js", "json", "md", "htm", "py", "go", "rs","txt");

    private String texte;
    private List<String> repertoires;
    private List<String> exclusions;
    private List<String> extensionsFichiers;

    public GrepParam() {
        exclusions = REPERTOIRES_EXCLUSION;
        extensionsFichiers = EXTENSIONS_FICHIERS_DEFAULT;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public List<String> getRepertoires() {
        return repertoires;
    }

    public void setRepertoires(List<String> repertoires) {
        this.repertoires = repertoires;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }

    public List<String> getExtensionsFichiers() {
        return extensionsFichiers;
    }

    public void setExtensionsFichiers(List<String> extensionsFichiers) {
        this.extensionsFichiers = extensionsFichiers;
    }
}
