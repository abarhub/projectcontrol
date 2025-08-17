package org.projectcontrol.server.vo;

import org.projectcontrol.server.enumeration.ModuleProjetEnum;

import java.time.LocalDateTime;
import java.util.Set;

public class Projet {

    private String id;
    private String nom;
    private String description;
    private String repertoire;
    private String fichierPom;
    private String packageJson;
    private String goMod;
    private String cargoToml;
    private ProjetPom projetPom;
    private Set<ModuleProjetEnum> modules;
    private ProjetNode projetNode;
    private ProjetGit projetGit;
    private LocalDateTime dateModification;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public ProjetPom getProjetPom() {
        return projetPom;
    }

    public void setProjetPom(ProjetPom projetPom) {
        this.projetPom = projetPom;
    }

    public Set<ModuleProjetEnum> getModules() {
        return modules;
    }

    public void setModules(Set<ModuleProjetEnum> modules) {
        this.modules = modules;
    }

    public ProjetNode getProjetNode() {
        return projetNode;
    }

    public void setProjetNode(ProjetNode projetNode) {
        this.projetNode = projetNode;
    }

    public ProjetGit getProjetGit() {
        return projetGit;
    }

    public void setProjetGit(ProjetGit projetGit) {
        this.projetGit = projetGit;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }
}
