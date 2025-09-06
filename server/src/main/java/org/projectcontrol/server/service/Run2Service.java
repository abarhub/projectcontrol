package org.projectcontrol.server.service;

import org.projectcontrol.core.service.RunService;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.dto.ReponseRunInitialDto;
import org.projectcontrol.server.dto.ReponseRunSuivanteDto;
import org.projectcontrol.server.recherche.ExecuteRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class Run2Service {


    private static final Logger LOGGER = LoggerFactory.getLogger(Run2Service.class);
    private final RunService runService;
    private final ProjetService projetService;
    private final ExecutorService executorService;
    private final AtomicLong count = new AtomicLong(1);
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Map<String, ExecuteRun> map = new ConcurrentHashMap<>();

    public Run2Service(RunService runService, ProjetService projetService) {
        this.runService = runService;
        this.projetService = projetService;
        executorService = Executors.newCachedThreadPool();
    }

    public ReponseRunInitialDto run(String groupId, String nomProjet, String action) {

        LOGGER.info("recherche groupe : {} - {} - {} ...", groupId, nomProjet, action);
        List<ProjetDto> listeGroupe = projetService.getProjetDto(groupId, null);
        LOGGER.info("recherche groupe : {} - {} - {} OK", groupId, nomProjet, action);

        if (listeGroupe == null || listeGroupe.size() != 1) {
            ReponseRunInitialDto resultatDto = new ReponseRunInitialDto();
            return resultatDto;
        }
        ProjetDto projet = listeGroupe.getFirst();

        var idStr = getId();
//        String repertoire = projet.getRepertoire();

        ExecuteRun executeRun = new ExecuteRun(idStr, runService, projet, action);
        map.put(idStr, executeRun);

        LOGGER.info("run : {} - {} ...", groupId, nomProjet);
        executorService.submit(() -> {
            executeRun.run();
            return 0;
        });

        ReponseRunInitialDto resultatDto = new ReponseRunInitialDto();
        resultatDto.setId(idStr);

        return resultatDto;
    }

    public ReponseRunSuivanteDto runSuite(String id) {
        if (map.containsKey(id)) {
            var recherche = map.get(id);
            ReponseRunSuivanteDto resultatDto = new ReponseRunSuivanteDto();
            resultatDto.setListeLignes(recherche.getResultatDtoList());
            resultatDto.setTerminer(recherche.isFini());
            return resultatDto;
        } else {
            ReponseRunSuivanteDto resultatDto = new ReponseRunSuivanteDto();
            resultatDto.setTerminer(true);
            return resultatDto;
        }
    }


    private String getId() {
        long id = count.getAndIncrement();
        var idStr = "" + id;
        return idStr;
    }
}
