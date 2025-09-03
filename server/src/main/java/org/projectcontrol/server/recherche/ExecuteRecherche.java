package org.projectcontrol.server.recherche;

import org.projectcontrol.core.service.GrepService;
import org.projectcontrol.core.utils.GrepParam;
import org.projectcontrol.core.utils.LignesRecherche;
import org.projectcontrol.server.dto.LigneResultatDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ExecuteRecherche {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteRecherche.class);

    private final String id;
    private final GrepService grepService;
    //    private final List<LigneResultatDto> resultatDtoList=new CopyOnWriteArrayList<>();
    private final BlockingQueue<LigneResultatDto> resultatDtoList = new LinkedBlockingQueue<>();
    private boolean fini = false;

    public ExecuteRecherche(String id, GrepService grepService) {
        this.id = id;
        this.grepService = grepService;
    }

    public void run(GrepParam grepParam, String repertoire) throws Exception {
        try {
            fini = false;
//            var res0 = grepService.search(grepParam)
//                    .collectList();
//            var res = res0.block();
//
//            Path repertoireProjet = Path.of(repertoire);
//            for (LignesRecherche ligne : res) {
//                var ligneResultatDto = convertie(ligne, repertoireProjet);
//                resultatDtoList.add(ligneResultatDto);
//            }
//            fini = true;
            grepService.search(grepParam)
                    .subscribe(ligne -> {
                        resultatDtoList.add(convertie(ligne, Path.of(repertoire)));
                    }, (error) -> {
                        LOGGER.error("Erreur lors de l'analyse du projet {}", id, error);
                    }, () -> {
                        fini = true;
                    });
            LOGGER.info("nb resultat : {}", resultatDtoList.size());
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'analyse du projet {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private LigneResultatDto convertie(LignesRecherche ligne, Path repertoireProjet) {
        var ligneResultatDto = new LigneResultatDto();
        ligneResultatDto.setNoLigne(ligne.noLigneDebut());
        ligneResultatDto.setLigne(ligne.lignes().getFirst());
        Path path = ligne.ficher();
        ligneResultatDto.setFichier(repertoireProjet.relativize(path).toString());
        ligneResultatDto.setRepertoireParent(repertoireProjet.toString());
        return ligneResultatDto;
    }

    public boolean isFini() {
        return fini;
    }

    public List<LigneResultatDto> getResultatDtoList() {
        List<LigneResultatDto> liste=new ArrayList<>();
        resultatDtoList.drainTo(liste);
        return liste;
    }

}
