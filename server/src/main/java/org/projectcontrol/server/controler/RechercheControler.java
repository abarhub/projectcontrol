package org.projectcontrol.server.controler;

import org.projectcontrol.server.dto.LigneResultatDto;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.dto.ReponseRechercheInitialDto;
import org.projectcontrol.server.dto.ReponseRechercheSuivanteDto;
import org.projectcontrol.server.service.RechercheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("api/recherche")
public class RechercheControler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RechercheControler.class);

    private final RechercheService rechercheService;

    public RechercheControler(RechercheService rechercheService) {
        this.rechercheService = rechercheService;
    }

    @GetMapping(path = "/{groupId}", produces = "application/json")
    public ReponseRechercheInitialDto getListProjet(@PathVariable String groupId,
                                                    @RequestParam String texte,
                                                    @RequestParam String typeRecherche,
                                                    @RequestParam(required = false) String projetId) throws IOException {
        LOGGER.info("recherche : {} - {}", groupId, texte);
        return rechercheService.recherche(groupId, texte, typeRecherche, projetId);
    }

    @GetMapping(path = "/suite/{id}", produces = "application/json")
    public ReponseRechercheSuivanteDto rechercheSuite(@PathVariable String id) throws IOException {
        LOGGER.info("rechercheSuite : {}", id);
        return rechercheService.rechercheSuite(id);
    }
}
