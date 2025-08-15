package org.projectcontrol.server.controler;

import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.service.ProjetService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/projet")
public class ProjetControler {

    private final ProjetService projetService;

    public ProjetControler(ProjetService projetService) {
        this.projetService = projetService;
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping(path = "", produces = "application/json")
    public List<ProjetDto> getProjet() {
        return projetService.getProjetDto();
    }

}
