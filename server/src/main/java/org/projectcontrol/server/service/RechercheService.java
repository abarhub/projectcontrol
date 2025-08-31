package org.projectcontrol.server.service;

import org.projectcontrol.core.service.GrepService;
import org.projectcontrol.core.utils.GrepCriteresRecherche;
import org.projectcontrol.core.utils.GrepParam;
import org.projectcontrol.server.dto.LigneResultatDto;
import org.projectcontrol.server.dto.ProjetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class RechercheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RechercheService.class);

    private final GrepService grepService;
    private final ProjetService projetService;

    public RechercheService(GrepService grepService, ProjetService projetService) {
        this.grepService = grepService;
        this.projetService = projetService;
    }

    public List<LigneResultatDto> recherche(String groupId, String texte, String typeRecherche) throws IOException {
        List<LigneResultatDto> resultat = new ArrayList<>();

        LOGGER.info("recherche groupe : {} - {} ...", groupId, texte);
        List<ProjetDto> listeGroupe = projetService.getProjetDto(groupId, null);
        LOGGER.info("recherche groupe : {} - {} OK", groupId, texte);

        if (listeGroupe == null || listeGroupe.size() != 1) {
            return resultat;
        }
        ProjetDto projet = listeGroupe.getFirst();

        String repertoire = projet.getRepertoire();
        GrepParam grepParam = new GrepParam();
        grepParam.setExclusions(GrepService.REPERTOIRES_EXCLUSION);
        grepParam.setExtensionsFichiers(GrepService.EXTENSIONS_FICHIERS_DEFAULT);
        grepParam.setRepertoires(List.of(repertoire));
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
            case "regex":
                criteresRecherche.setRegex(List.of(texte));
            default:
                throw new IllegalArgumentException("typeRecherche inconnu : " + typeRecherche);
        }

        grepParam.setCriteresRecherche(criteresRecherche);

        LOGGER.info("search : {} - {} ...", groupId, texte);
        var res = grepService.search(grepParam)
                .blockingIterable();
        LOGGER.info("search : {} - {} OK", groupId, texte);

        Path repertoireProjet = Path.of(repertoire);
        for (org.projectcontrol.core.utils.LignesRecherche ligne : res) {
            var ligneResultatDto = new LigneResultatDto();
            ligneResultatDto.setNoLigne(ligne.noLigneDebut());
            ligneResultatDto.setLigne(ligne.lignes().getFirst());
            Path path = ligne.ficher();
            ligneResultatDto.setFichier(repertoireProjet.relativize(path).toString());
            ligneResultatDto.setRepertoireParent(repertoire);
            resultat.add(ligneResultatDto);
        }
        LOGGER.info("nb resultat : {}", resultat.size());

        return resultat;
    }
}
