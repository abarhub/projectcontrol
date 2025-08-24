package org.projectcontrol.server.controler;

import org.projectcontrol.server.dto.ListVersionDto;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.service.ProjetService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("api/maj-version")
public class MajVersionControler {

    private final ProjetService projetService;

    public MajVersionControler(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping(path = "/{groupId}/{nomProjet}", produces = "application/json")
    public ListVersionDto getListeVersions(@PathVariable String groupId,
                                           @PathVariable String nomProjet) {
        return projetService.getListeVersion(groupId, nomProjet);
    }
}
