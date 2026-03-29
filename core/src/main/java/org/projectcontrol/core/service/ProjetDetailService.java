package org.projectcontrol.core.service;

import org.projectcontrol.core.vo.projet.DetailProjet;
import org.projectcontrol.core.vo.projet.MavenDependency;
import org.projectcontrol.core.vo.projet.MavenProjet;
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

    public ProjetDetailService(EffectivePomReaderService reader) {
        this.reader = reader;
    }

    public DetailProjet getProjetDetail(Path repertoire) {
        MavenProjet mavenProjet = analysePom(repertoire);

        return new DetailProjet(mavenProjet);
    }

    private MavenProjet analysePom(Path repertoire) {
        var pomFile = repertoire.resolve("pom.xml");
        if (Files.exists(pomFile)) {
            try {
                MavenProjet pom = reader.readEffectivePom(new File("."));



                return pom;

            } catch (Exception e) {
                LOGGER.error("erreur pour récupérer l'effective pom de {}", repertoire, e);
            }
        }
        return null;
    }

}
