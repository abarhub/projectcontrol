package org.projectcontrol.server.service;

import org.apache.commons.lang3.StringUtils;
import org.projectcontrol.core.service.GrepService;
import org.projectcontrol.core.utils.GrepCriteresRecherche;
import org.projectcontrol.core.utils.GrepParam;
import org.projectcontrol.server.dto.LigneResultatDto;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.dto.ReponseRechercheInitialDto;
import org.projectcontrol.server.dto.ReponseRechercheSuivanteDto;
import org.projectcontrol.server.recherche.ExecuteRecherche;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RechercheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RechercheService.class);

    private final GrepService grepService;
    private final ProjetService projetService;

    private final Map<String, ExecuteRecherche> map = new ConcurrentHashMap<>();
    private final AtomicLong count = new AtomicLong(1);
    private final ExecutorService executorService;

    public RechercheService(GrepService grepService, ProjetService projetService) {
        this.grepService = grepService;
        this.projetService = projetService;
        executorService = Executors.newCachedThreadPool();
    }

    public ReponseRechercheInitialDto recherche(String groupId, String texte, String typeRecherche,
                                                String projetId, int nbLignesAutour) throws IOException {
        List<LigneResultatDto> resultat = new ArrayList<>();

        LOGGER.info("recherche groupe : {} - {} ...", groupId, texte);
        List<ProjetDto> listeGroupe = projetService.getProjetDto(groupId, (StringUtils.isBlank(projetId) ? null : projetId));
        LOGGER.info("recherche groupe : {} - {} OK", groupId, texte);

        if (listeGroupe == null || listeGroupe.size() != 1) {
            ReponseRechercheInitialDto resultatDto = new ReponseRechercheInitialDto();
            return resultatDto;
        }
        ProjetDto projet = listeGroupe.getFirst();

        String repertoire = projet.getRepertoire();
        GrepParam grepParam = new GrepParam();
        grepParam.setExclusions(GrepService.REPERTOIRES_EXCLUSION);
        grepParam.setExtensionsFichiers(GrepService.EXTENSIONS_FICHIERS_DEFAULT);
        grepParam.setRepertoires(List.of(repertoire));
        if (nbLignesAutour > 0) {
            grepParam.setNbLignesAutour(nbLignesAutour);
        }
        GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
        switch (typeRecherche) {
            case "xpath":
                criteresRecherche.setXpath(List.of(texte));
                break;
            case "chemin":
                criteresRecherche.setChamps(List.of(texte));
                break;
            case "texte":
                criteresRecherche.setTexte(List.of(texte));
                break;
            case "regexp":
                criteresRecherche.setRegex(List.of(texte));
                break;
            default:
                throw new IllegalArgumentException("typeRecherche inconnu : " + typeRecherche);
        }

        grepParam.setCriteresRecherche(criteresRecherche);

        var idStr = getId();
        ExecuteRecherche executeRecherche = new ExecuteRecherche(idStr, grepService);

        LOGGER.info("search : {} - {} ...", groupId, texte);
        executorService.submit(() -> {
            executeRecherche.run(grepParam, repertoire);
            return 0;
        });
        map.put(idStr, executeRecherche);
        LOGGER.info("search : {} - {} OK", groupId, texte);

        ReponseRechercheInitialDto resultatDto = new ReponseRechercheInitialDto();
        resultatDto.setId(idStr);

        return resultatDto;
    }

    public ReponseRechercheSuivanteDto rechercheSuite(String id) {
        if (map.containsKey(id)) {
            var recherche = map.get(id);
            ReponseRechercheSuivanteDto resultatDto = new ReponseRechercheSuivanteDto();
            resultatDto.setListeLignes(recherche.getResultatDtoList());
            resultatDto.setTerminer(recherche.isFini());
            return resultatDto;
        } else {
            ReponseRechercheSuivanteDto resultatDto = new ReponseRechercheSuivanteDto();
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
