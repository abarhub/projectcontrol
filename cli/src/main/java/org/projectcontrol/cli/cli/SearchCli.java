package org.projectcontrol.cli.cli;

import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.collections4.CollectionUtils;
import org.projectcontrol.cli.utils.PicocliListConverter;
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
    private static final Logger LOGGER_SANS_FORMAT = LoggerFactory.getLogger("ConsoleSansFormat");

    @CommandLine.Option(names = "--texte", description = "Le texte recherché", required = true)
    private String texte;

    @CommandLine.Option(names = "--repertoires", description = "Les répertoires. Par défaut : ${DEFAULT-VALUE}",
            defaultValue = ".", converter = PicocliListConverter.class)
    private List<String> repertoires;

    @CommandLine.Option(names = "--exclusions", description = "Les répertoires à exclure. Par défaut : ${DEFAULT-VALUE}",
            defaultValue = GrepService.REPERTOIRES_EXCLUSION_STR, converter = PicocliListConverter.class)
    private List<String> exclusions;

    @CommandLine.Option(names = "--extensionsFichiers", description = "Les extensions de fichiers pour la recherche. " +
            "Par défaut : ${DEFAULT-VALUE}",
            defaultValue = GrepService.EXTENSIONS_FICHIERS_DEFAULT_STR, converter = PicocliListConverter.class)
    private List<String> extensionsFichiers;

    @CommandLine.Option(names = "--resultatSansFormatage", description = "Affichage du résultat sans formatage. " +
            "Par défaut : ${DEFAULT-VALUE}", defaultValue = "true")
    private boolean resultatSansFormatageLog;

    @CommandLine.Option(names = "--quiet", description = "Le nom du fichier et la ligne ne sont pas affichés. " +
            "Par défaut : ${DEFAULT-VALUE}", defaultValue = "false")
    private boolean quiet;

    @CommandLine.Option(names = "--nbLignesAutour", description = "Affiche X lignes autour. " +
            "Par défaut : ${DEFAULT-VALUE}", defaultValue = "0")
    private int nbLignesAutour;

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
            if( nbLignesAutour > 0 ){
                grepParam.setNbLignesAutour(nbLignesAutour);
            }
            var reponse = grepService.search(grepParam);

            Logger logger = resultatSansFormatageLog ? LOGGER_SANS_FORMAT : LOGGER;

            Disposable res = null;
            try {
                res = reponse.subscribe(ligneRecherche -> {
                    if (quiet) {
                        logger.info("{}", ligneRecherche.lignes().getFirst());
                    } else {
                        logger.info("{}:{}:{}", ligneRecherche.ficher(), ligneRecherche.noLigneDebut(), ligneRecherche.lignes().getFirst());
                    }
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
