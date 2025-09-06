package org.projectcontrol.server.recherche;

import org.projectcontrol.core.service.RunService;
import org.projectcontrol.server.dto.ProjetDto;
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

    public ExecuteRun(String id, RunService runService, ProjetDto projet) {
        this.id = id;
        this.runService = runService;
        repertoire=projet.getRepertoire();
    }

    public void run() throws IOException, InterruptedException {

        LOGGER.info("debut run ...");
        fini = false;
        var fichier=repertoire+"/pom.xml";
        if(false) {
            runService.runCommand(x -> {
                try {
                    LOGGER.info("ligne: {}", x);
                    resultat.put(x.line());
                } catch (InterruptedException e) {
                    LOGGER.error("error", e);
                    throw new RuntimeException(e);
                }
            }, "cmd", "/C", "mvn", "dependency:tree", "-f", fichier);
        } else {
            var res = runService.runCommand("cmd", "/C", "mvn", "dependency:tree", "-f", fichier);
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
