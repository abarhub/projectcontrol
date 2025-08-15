package org.projectcontrol.server.dto;

import java.util.List;

public class ProjetDto {

    private String nom;
    private String description;
    private String repertoire;
    private String fichierPom;
    private String packageJson;
    private String goMod;
    private String cargoToml;
    private ArtifactDto parent;
    private ArtifactDto artifact;
    private List<ProjetDto> projetEnfants;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRepertoire() {
        return repertoire;
    }

    public void setRepertoire(String repertoire) {
        this.repertoire = repertoire;
    }

    public String getFichierPom() {
        return fichierPom;
    }

    public void setFichierPom(String fichierPom) {
        this.fichierPom = fichierPom;
    }

    public String getPackageJson() {
        return packageJson;
    }

    public void setPackageJson(String packageJson) {
        this.packageJson = packageJson;
    }

    public String getGoMod() {
        return goMod;
    }

    public void setGoMod(String goMod) {
        this.goMod = goMod;
    }

    public String getCargoToml() {
        return cargoToml;
    }

    public void setCargoToml(String cargoToml) {
        this.cargoToml = cargoToml;
    }

    public ArtifactDto getParent() {
        return parent;
    }

    public void setParent(ArtifactDto parent) {
        this.parent = parent;
    }

    public ArtifactDto getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactDto artifact) {
        this.artifact = artifact;
    }

    public List<ProjetDto> getProjetEnfants() {
        return projetEnfants;
    }

    public void setProjetEnfants(List<ProjetDto> projetEnfants) {
        this.projetEnfants = projetEnfants;
    }
}
