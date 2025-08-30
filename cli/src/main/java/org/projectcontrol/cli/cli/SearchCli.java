package org.projectcontrol.cli.cli;

import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.collections4.CollectionUtils;
import org.projectcontrol.core.service.GrepService;
import org.projectcontrol.core.utils.GrepParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@Service
@CommandLine.Command(name = "search")
public class SearchCli implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCli.class);

    @CommandLine.Option(names = "--texte", description = "Le texte recherché", required = true)
    private String texte;

    @CommandLine.Option(names = "--repertoires", description = "Les répertoires")
    private List<String> repertoires = List.of(".");

    @CommandLine.Option(names = "--exclusions", description = "Les répertoires à exclure")
    private List<String> exclusions;

    @CommandLine.Option(names = "--extensionsFichiers", description = "Les extensions de fichiers pour la recherche")
    private List<String> extensionsFichiers;

    private final GrepService grepService;

    public SearchCli(GrepService grepService) {
        this.grepService = grepService;
    }

    @Override
    public Integer call() throws Exception {
        int resultat = 0;
        LOGGER.info("recherche texte:{} repertoires:{}", texte, repertoires);
        try {
            GrepParam grepParam = new GrepParam();
            grepParam.setTexte(texte);
            grepParam.setRepertoires(repertoires);
            if (CollectionUtils.isNotEmpty(exclusions)) {
                grepParam.setExclusions(exclusions);
            }
            if (CollectionUtils.isNotEmpty(extensionsFichiers)) {
                grepParam.setExtensionsFichiers(extensionsFichiers);
            }
            var reponse = grepService.search(grepParam);

            Disposable res = null;
            try {
                res = reponse.subscribe(ligneRecherche -> {
//                    LOGGER.info("Ligne recherche:{}", ligneRecherche);
                    LOGGER.info("{}:{}:{}",ligneRecherche.ficher(),ligneRecherche.noLigneDebut(),ligneRecherche.lignes().getFirst());
                });
            } finally {
                if (res != null) {
                    res.dispose();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Erreur", e);
            resultat = 1;
        }
        return resultat;
    }
}
