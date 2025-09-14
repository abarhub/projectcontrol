package org.projectcontrol.server.controler;

import org.projectcontrol.server.dto.GroupeProjetDto;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.service.ProjetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("api/projet")
public class ProjetControler {

    private final ProjetService projetService;

    public ProjetControler(ProjetService projetService) {
        this.projetService = projetService;
    }


    @GetMapping(path = "/from-groupId/{groupId}", produces = "application/json")
    public List<ProjetDto> getListProjet(@PathVariable String groupId) {
        return projetService.getProjetDtoFromGroupId(groupId);
    }

    @GetMapping(path = "/{groupId}/{nomProjet}", produces = "application/json")
    public ProjetDto getProjet(@PathVariable String groupId,
                               @PathVariable String nomProjet) {
        var liste = projetService.getProjetDto(groupId, nomProjet);
        if (liste != null && !liste.isEmpty()) {
            return liste.getFirst();
        } else {
            return null;
        }
    }

    @GetMapping(path = "groupe-projets", produces = "application/json")
    public GroupeProjetDto getGroupeProjets() {
        return projetService.getGroupeProjets();
    }

}
