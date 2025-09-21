package org.projectcontrol.server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Map<String, ProjetProperties> listeProjets;
    private MajProperties maj;

    public Map<String, ProjetProperties> getListeProjets() {
        return listeProjets;
    }

    public void setListeProjets(Map<String, ProjetProperties> listeProjets) {
        this.listeProjets = listeProjets;
    }

    public MajProperties getMaj() {
        return maj;
    }

    public void setMaj(MajProperties maj) {
        this.maj = maj;
    }
}
