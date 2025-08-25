package org.projectcontrol.server.controler;


import org.projectcontrol.server.dto.ChangementConfigDto;
import org.projectcontrol.server.service.ProjetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("api/changement-config")
public class ChangementConfigControler {


    private static final Logger LOGGER = LoggerFactory.getLogger(ChangementConfigControler.class);
    private final ProjetService projetService;

    public ChangementConfigControler(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping(path = "/{groupId}/{nomProjet}", produces = "application/json")
    public ChangementConfigDto getChangementConfig(@PathVariable String groupId,
                                                   @PathVariable String nomProjet,
                                                   @RequestParam String commitInitial,
                                                   @RequestParam String commitFinal) throws Exception {
        LOGGER.info("changement de config : {} - {} - {} - {}", groupId, nomProjet, commitInitial, commitFinal);
        return projetService.getChangementConfig(groupId, nomProjet, commitInitial, commitFinal);
    }
}
