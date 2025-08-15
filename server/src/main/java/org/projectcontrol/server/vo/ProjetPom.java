package org.projectcontrol.server.vo;

import java.util.List;
import java.util.Map;

public class ProjetPom {

    private String nom;
    private ArtefactMaven parent;
    private ArtefactMaven artifact;
    private Map<String,String> properties;
    private List<ArtefactMaven> dependencies;
    private List<ProjetPom> projetPomEnfants;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public ArtefactMaven getParent() {
        return parent;
    }

    public void setParent(ArtefactMaven parent) {
        this.parent = parent;
    }

    public ArtefactMaven getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtefactMaven artifact) {
        this.artifact = artifact;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<ArtefactMaven> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ArtefactMaven> dependencies) {
        this.dependencies = dependencies;
    }

    public List<ProjetPom> getProjetPomEnfants() {
        return projetPomEnfants;
    }

    public void setProjetPomEnfants(List<ProjetPom> projetPomEnfants) {
        this.projetPomEnfants = projetPomEnfants;
    }
}
