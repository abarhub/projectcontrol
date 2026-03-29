package org.projectcontrol.core.vo.projet;

import java.util.ArrayList;
import java.util.List;

public class MavenDependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String scope;
    private boolean optional;

    // Les sous-dépendances transitives (arbre récursif)
    private List<MavenDependency> transitiveDependencies = new ArrayList<>();

    public MavenDependency(String groupId, String artifactId,
                           String version, String type,
                           String scope, boolean optional) {
        this.groupId    = groupId;
        this.artifactId = artifactId;
        this.version    = version;
        this.type       = type != null ? type : "jar";
        this.scope      = scope != null ? scope : "compile";
        this.optional   = optional;
    }

    /** Clé unique pour retrouver une dépendance dans l'arbre */
    public String getKey() {
        return groupId + ":" + artifactId + ":" + type + ":" + version + ":" + scope;
    }

    public void addTransitive(MavenDependency child) {
        this.transitiveDependencies.add(child);
    }

    // --- Getters / Setters ---
    public String getGroupId()    { return groupId; }
    public String getArtifactId() { return artifactId; }
    public String getVersion()    { return version; }
    public String getType()       { return type; }
    public String getScope()      { return scope; }
    public boolean isOptional()   { return optional; }
    public List<MavenDependency> getTransitiveDependencies() { return transitiveDependencies; }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version + " [" + scope + "]"
                + (transitiveDependencies.isEmpty() ? "" : " (" + transitiveDependencies.size() + " transitives)");
    }
}
