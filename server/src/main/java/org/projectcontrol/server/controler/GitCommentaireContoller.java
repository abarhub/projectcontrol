package org.projectcontrol.server.controler;


import org.projectcontrol.server.dto.LigneGitDto;
import org.projectcontrol.server.service.ProjetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/git-commentaire")
public class GitCommentaireContoller {


    private static final Logger LOGGER = LoggerFactory.getLogger(GitCommentaireContoller.class);
    private final ProjetService projetService;

    public GitCommentaireContoller(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping(path = "/{groupId}/{nomProjet}", produces = "application/json")
    public List<LigneGitDto> getGitCommentaire(@PathVariable String groupId,
                                               @PathVariable String nomProjet,
                                               @RequestParam int nbCommit) throws Exception {
        LOGGER.info("git commentaire : {} - {} - {}", groupId, nomProjet, nbCommit);
        return projetService.getGitCommentaire(groupId, nomProjet, nbCommit);
    }

}
