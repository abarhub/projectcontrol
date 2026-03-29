package org.projectcontrol.core.vo.projet;

import java.nio.file.Path;
import java.util.Map;

public class NodeProjet {

    private String name;
    private String version;
    private Map<String, String> scripts;
    private Map<String, String> dependencies;
    private Map<String, String> devDependencies;
    private String sousRepertoire;
    private String fichierPackageJson;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getScripts() {
        return scripts;
    }

    public void setScripts(Map<String, String> scripts) {
        this.scripts = scripts;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, String> getDevDependencies() {
        return devDependencies;
    }

    public void setDevDependencies(Map<String, String> devDependencies) {
        this.devDependencies = devDependencies;
    }

    public String getSousRepertoire() {
        return sousRepertoire;
    }

    public void setSousRepertoire(String sousRepertoire) {
        this.sousRepertoire = sousRepertoire;
    }

    public String getFichierPackageJson() {
        return fichierPackageJson;
    }

    public void setFichierPackageJson(String fichierPackageJson) {
        this.fichierPackageJson = fichierPackageJson;
    }
}
