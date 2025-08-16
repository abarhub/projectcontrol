package org.projectcontrol.server.dto;

import java.util.Map;

public class InfoNodeDto {
    private String nom;
    private String version;
    private Map<String, String> script;
    private Map<String, String> dependencies;
    private Map<String, String> devDependencies;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getScript() {
        return script;
    }

    public void setScript(Map<String, String> script) {
        this.script = script;
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
}
