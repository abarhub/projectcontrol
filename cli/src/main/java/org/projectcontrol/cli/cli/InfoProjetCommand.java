package org.projectcontrol.cli.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.projectcontrol.core.service.ProjetDetailService;
import org.projectcontrol.core.vo.projet.DetailProjet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "infoProjet")
public class InfoProjetCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoProjetCommand.class);

    private final ProjetDetailService projetDetailService;

    @CommandLine.Option(names = "--output", description = "output file")
    private String output;

    @CommandLine.Option(names = "--directory", description = "Répertoire du projet")
    private String directory;

    public InfoProjetCommand(ProjetDetailService projetDetailService) {
        this.projetDetailService = projetDetailService;
    }

    @Override
    public Integer call() throws Exception {

        try {
            Path repertoire;
            Path fichierSortie;

            if (StringUtils.isNotBlank(directory)) {
                repertoire = Path.of(directory);
            } else {
                repertoire = Path.of(".");
            }

            if (Files.notExists(repertoire)) {
                throw new IllegalArgumentException("Le répertoire du projet n'existe pas : " + repertoire);
            }

            if (StringUtils.isBlank(output)) {
                throw new IllegalArgumentException("Le fichier de sortie est obligatoire");
            }
            fichierSortie = Path.of(output);

            LOGGER.info("Analyse du projet dans le répertoire : {}", repertoire);

            var resultat = projetDetailService.getProjetDetail(repertoire);

            ecritureFichier(resultat, fichierSortie);

            return 0;
        } catch (Exception e) {
            LOGGER.error("Error analyzing pom file", e);
            return 1;
        }
    }

    private void ecritureFichier(DetailProjet resultat, Path output) {

        LOGGER.info("Ecriture du fichier de sortie : {} ...", output);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), resultat);

            LOGGER.info("Ecriture du fichier de sortie : {} OK", output);

        } catch (Exception e) {
            LOGGER.error("Erreur pour écrire le fichier de sortie '{}'", output, e);
        }
    }
}
