package org.projectcontrol.server.recherche;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectcontrol.core.service.RunService;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ExecuteRun {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteRun.class);

    private final String id;
    private final BlockingQueue<String> resultat = new LinkedBlockingQueue<>();
    private final RunService runService;
    private boolean fini = false;
    private final String repertoire;
    private final String action;
    private final ApplicationProperties applicationProperties;

    public ExecuteRun(String id, RunService runService, ProjetDto projet, String action, ApplicationProperties applicationProperties) {
        this.id = id;
        this.runService = runService;
        repertoire = projet.getRepertoire();
        this.action = action;
        this.applicationProperties = applicationProperties;
    }

    public void run() throws IOException, InterruptedException {

        LOGGER.info("debut run ...");
        fini = false;
        var fichier = repertoire + "/pom.xml";
        if (StringUtils.isEmpty(action)) {
            throw new RuntimeException("Action invalide: " + action);
        }
        List<String> listeCommande = getListeCommande(action, fichier);
        if (CollectionUtils.isEmpty(listeCommande)) {
            throw new RuntimeException("Commande invalide: " + listeCommande);
        }
        if (false) {
            runService.runCommand(x -> {
                try {
                    LOGGER.info("ligne: {}", x);
                    resultat.put(x.line());
                } catch (InterruptedException e) {
                    LOGGER.error("error", e);
                    throw new RuntimeException(e);
                }
            }, listeCommande.toArray(new String[0]));
        } else {
            var res = runService.runCommand(listeCommande.toArray(new String[0]));
            LOGGER.info("fin run ...");
            LOGGER.info("subscribe ...");
            var disposable = res.subscribe(x -> {
                        try {
                            LOGGER.info("ligne: {}", x);
                            resultat.put(x.line());
                        } catch (InterruptedException e) {
                            LOGGER.error("error", e);
                            throw new RuntimeException(e);
                        }
                    }, error -> {
                        LOGGER.error("error", error);
                    },
                    () -> {
                        fini = true;
                        LOGGER.info("fin run ...");
                    });
            LOGGER.info("subscribe ok");
            LOGGER.info("dispose ...");
            disposable.dispose();
            LOGGER.info("dispose ok");
        }
    }

    private List<String> getListeCommande(String action, String fichier) {
        if (action.equals("dependencyTree")) {
            return List.of("cmd", "/C", "mvn", "dependency:tree", "-f", fichier);
        } else if (action.equals("dependencyAnalyse")) {
            return List.of("cmd", "/C", "mvn", "dependency:analyze", "-f", fichier);
        } else {
            var map = applicationProperties.getRun();
            if (map != null && map.containsKey(action)) {
                var commande = map.get(action);
                List<String> commandes = new ArrayList<>();
                if (commande.isShell()) {
                    commandes.addAll(List.of("cmd", "/C"));
                }
                commandes.addAll(commande.getCommande());
                commandes = commandes.stream()
                        .map(x -> StringUtils.replace(x, ":REPERTOIRE_PROJET:", repertoire))
                        .toList();
                return commandes;
            }

            throw new RuntimeException("Action invalide: " + action);
        }
    }

    public List<String> getResultatDtoList() {
        List<String> liste = new ArrayList<>();
        resultat.drainTo(liste);
        return liste;
    }

    public boolean isFini() {
        return fini;
    }

    public void setFini(boolean fini) {
        this.fini = fini;
    }
}
