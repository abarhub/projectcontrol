package org.projectcontrol.server.dto;

import org.projectcontrol.server.enumeration.ModuleProjetEnum;
import org.projectcontrol.server.vo.ArtefactMaven;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjetDto {

    private String nom;
    private String description;
    private String repertoire;
//    private String fichierPom;
//    private String packageJson;
//    private String goMod;
//    private String cargoToml;
    private ArtifactDto parent;
    private ArtifactDto artifact;
    private Map<String,String> properties;
    private List<ArtefactMaven> dependencies;
    private List<ProjetDto> projetEnfants;
    private Set<ModuleProjetEnum> modules;
    private InfoNodeDto infoNode;
    private InfoGitDto infoGit;
    private LocalDateTime dateModification;
    private Map<String, String> detailModules;

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

//    public String getFichierPom() {
//        return fichierPom;
//    }
//
//    public void setFichierPom(String fichierPom) {
//        this.fichierPom = fichierPom;
//    }
//
//    public String getPackageJson() {
//        return packageJson;
//    }
//
//    public void setPackageJson(String packageJson) {
//        this.packageJson = packageJson;
//    }
//
//    public String getGoMod() {
//        return goMod;
//    }
//
//    public void setGoMod(String goMod) {
//        this.goMod = goMod;
//    }
//
//    public String getCargoToml() {
//        return cargoToml;
//    }
//
//    public void setCargoToml(String cargoToml) {
//        this.cargoToml = cargoToml;
//    }

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

    public List<ProjetDto> getProjetEnfants() {
        return projetEnfants;
    }

    public void setProjetEnfants(List<ProjetDto> projetEnfants) {
        this.projetEnfants = projetEnfants;
    }

    public Set<ModuleProjetEnum> getModules() {
        return modules;
    }

    public void setModules(Set<ModuleProjetEnum> modules) {
        this.modules = modules;
    }

    public InfoNodeDto getInfoNode() {
        return infoNode;
    }

    public void setInfoNode(InfoNodeDto infoNode) {
        this.infoNode = infoNode;
    }

    public InfoGitDto getInfoGit() {
        return infoGit;
    }

    public void setInfoGit(InfoGitDto infoGit) {
        this.infoGit = infoGit;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public Map<String, String> getDetailModules() {
        return detailModules;
    }

    public void setDetailModules(Map<String, String> detailModules) {
        this.detailModules = detailModules;
    }
}
