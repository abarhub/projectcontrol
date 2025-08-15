package org.projectcontrol.server.controler;

import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.service.ProjetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("api/projet")
public class ProjetControler {

    private final ProjetService projetService;

    public ProjetControler(ProjetService projetService) {
        this.projetService = projetService;
    }


    @GetMapping(path = "", produces = "application/json")
    public List<ProjetDto> getListProjet() {
        return projetService.getProjetDto(null);
    }

    @GetMapping(path = "/{nomProjet}", produces = "application/json")
    public ProjetDto getProjet(@PathVariable String nomProjet) {
        var liste= projetService.getProjetDto(nomProjet);
        if(liste!=null && !liste.isEmpty()){
            return liste.getFirst();
        } else {
            return null;
        }
    }

}
