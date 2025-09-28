package org.projectcontrol.server.controler;

import org.projectcontrol.server.dto.ReponseRunInitialDto;
import org.projectcontrol.server.dto.ReponseRunSuivanteDto;
import org.projectcontrol.server.dto.RunConfigDto;
import org.projectcontrol.server.service.Run2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/run")
public class RunControler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunControler.class);

    private final Run2Service runService;

    public RunControler(Run2Service runService) {
        this.runService = runService;
    }

    @GetMapping(path = "/{groupId}/{nomProjet}", produces = "application/json")
    public ReponseRunInitialDto runInitialise(@PathVariable String groupId,
                                              @PathVariable String nomProjet,
                                              @RequestParam String action) throws IOException {
        LOGGER.info("runInitialise : {} - {} - {}", groupId, nomProjet, action);
        return runService.run(groupId, nomProjet, action);
    }

    @GetMapping(path = "/suite/{id}", produces = "application/json")
    public ReponseRunSuivanteDto runSuite(@PathVariable String id) throws IOException {
        LOGGER.info("runSuite : {}", id);
        return runService.runSuite(id);
    }

    @GetMapping(value = "/liste-run-config", produces = "application/json")
    public List<RunConfigDto> listeConfig() throws IOException {
        LOGGER.info("listeConfig");
        return runService.getListeRun();
    }
}
