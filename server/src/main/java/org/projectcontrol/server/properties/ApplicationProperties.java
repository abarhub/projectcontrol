package org.projectcontrol.server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Map<String, ProjetProperties> listeProjets;

    public Map<String, ProjetProperties> getListeProjets() {
        return listeProjets;
    }

    public void setListeProjets(Map<String, ProjetProperties> listeProjets) {
        this.listeProjets = listeProjets;
    }
}
