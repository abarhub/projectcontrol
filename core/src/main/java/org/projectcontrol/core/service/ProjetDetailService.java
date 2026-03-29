package org.projectcontrol.core.service;

import org.projectcontrol.core.vo.projet.DetailProjet;
import org.projectcontrol.core.vo.projet.MavenProjet;
import org.projectcontrol.core.vo.projet.NodeProjet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ProjetDetailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjetDetailService.class);

    private final EffectivePomReaderService reader;

    private final NodeReaderService nodeReaderService;

    public ProjetDetailService(EffectivePomReaderService reader, NodeReaderService nodeReaderService) {
        this.reader = reader;
        this.nodeReaderService = nodeReaderService;
    }

    public DetailProjet getProjetDetail(Path repertoire) {
        MavenProjet mavenProjet = analysePom(repertoire);

        List<NodeProjet> nodeProjets = nodeReaderService.analyse(repertoire);

        return new DetailProjet(mavenProjet, nodeProjets);
    }


    private MavenProjet analysePom(Path repertoire) {
        var pomFile = repertoire.resolve("pom.xml");
        if (Files.exists(pomFile)) {
            try {
                return reader.readEffectivePom(new File("."));
            } catch (Exception e) {
                LOGGER.error("erreur pour récupérer l'effective pom de {}", repertoire, e);
            }
        }
        return null;
    }

}
