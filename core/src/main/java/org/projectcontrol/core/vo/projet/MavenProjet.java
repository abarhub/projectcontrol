package org.projectcontrol.core.vo.projet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MavenProjet {

    private ArtifactInfo parent;
    private ArtifactInfo artifact;
    private String name;
    private String description;
    private Map<String, String> properties = new LinkedHashMap<>();
    private List<MavenDependency> dependencies = new ArrayList<>();
    private List<MavenProjet> modules = new ArrayList<>();  // ← récursif
    private List<MavenProfile> profiles = new ArrayList<>();
    private String fichierMaven;

    public ArtifactInfo getParent() {
        return parent;
    }

    public void setParent(ArtifactInfo parent) {
        this.parent = parent;
    }

    public ArtifactInfo getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactInfo artifact) {
        this.artifact = artifact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<MavenDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<MavenDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<MavenProjet> getModules() {
        return modules;
    }

    public void setModules(List<MavenProjet> modules) {
        this.modules = modules;
    }

    public List<MavenProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<MavenProfile> profiles) {
        this.profiles = profiles;
    }

    public String getFichierMaven() {
        return fichierMaven;
    }

    public void setFichierMaven(String fichierMaven) {
        this.fichierMaven = fichierMaven;
    }
}
