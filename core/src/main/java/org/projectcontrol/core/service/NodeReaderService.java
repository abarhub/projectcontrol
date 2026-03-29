package org.projectcontrol.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.projectcontrol.core.vo.projet.NodeProjet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class NodeReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeReaderService.class);

    public List<NodeProjet> analyse(Path repertoire) {
        return analyseNode(repertoire);
    }

    private List<NodeProjet> analyseNode(Path repertoire) {
        List<NodeProjet> nodeProjets = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        Path packageJson = repertoire.resolve("package.json");


        var resultat = analysePackageJson(mapper, packageJson);
        if (resultat != null) {
            nodeProjets.add(resultat);
        }
        try (var liste = Files.list(repertoire)) {
            liste.filter(Files::isDirectory)
                    .forEach(p -> {
                        var packageJson2 = p.resolve("package.json");
                        var resultat2 = analysePackageJson(mapper, packageJson2);
                        if (resultat2 != null) {
                            resultat2.setSousRepertoire(p.getFileName().toString());
                            nodeProjets.add(resultat2);
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("erreur pour lire le package.json de {}", repertoire, e);
        }
        return nodeProjets;
    }

    private static NodeProjet analysePackageJson(ObjectMapper mapper, Path packageJson) {
        if (Files.exists(packageJson)) {
            try {
                var json = mapper.readTree(packageJson.toFile());
                NodeProjet nodeProjet = new NodeProjet();
                nodeProjet.setFichierPackageJson(packageJson.toAbsolutePath().toString());
                if (json.has("version")) {
                    nodeProjet.setVersion(json.get("version").asText());
                }
                if (json.has("name")) {
                    nodeProjet.setName(json.get("name").asText());
                }
                if (json.has("scripts")) {
                    var scripts = json.get("scripts");
                    Map<String, String> map = new TreeMap<>();
                    for (Map.Entry<String, JsonNode> tmp : scripts.properties()) {
                        map.put(tmp.getKey(), tmp.getValue().asText());
                    }
                    nodeProjet.setScripts(map);
                }
                if (json.has("dependencies")) {
                    var dependencies = json.get("dependencies");
                    Map<String, String> map = new TreeMap<>();
                    for (Map.Entry<String, JsonNode> tmp : dependencies.properties()) {
                        map.put(tmp.getKey(), tmp.getValue().asText());
                    }
                    nodeProjet.setDependencies(map);
                }
                if (json.has("devDependencies")) {
                    var scripts = json.get("devDependencies");
                    Map<String, String> map = new TreeMap<>();
                    for (Map.Entry<String, JsonNode> tmp : scripts.properties()) {
                        map.put(tmp.getKey(), tmp.getValue().asText());
                    }
                    nodeProjet.setDevDependencies(map);
                }
                return nodeProjet;
            } catch (Exception e) {
                LOGGER.error("erreur pour lire le package.json de {}", packageJson, e);
            }
        }
        return null;
    }
}
