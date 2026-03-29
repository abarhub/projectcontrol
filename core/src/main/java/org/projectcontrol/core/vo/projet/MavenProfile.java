package org.projectcontrol.core.vo.projet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenProfile {
    private String id;
    private List<MavenDependency> dependencies = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();

    public MavenProfile(String id) { this.id = id; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<MavenDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<MavenDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
