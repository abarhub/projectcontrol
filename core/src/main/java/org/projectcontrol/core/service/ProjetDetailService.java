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
//                String[] commande = List.of("mvn", "help:effective-pom", "-Doutput=project.groupId", "-DforceStdout").toArray(String[]::new);
//                var liste = runService.runCommand(commande).collectList();
//                EffectivePomReaderService reader = new EffectivePomReaderService();
                MavenProjet pom = reader.readEffectivePom(new File("."));

                LOGGER.info("=== {} ===", pom.getArtifact());
                printDependencyTree(pom.getDependencies(), 0);

                LOGGER.info("=== Modules ===");
                for (MavenProjet module : pom.getModules()) {
                    LOGGER.info("  Module : {}", module.getArtifact());
                    printDependencyTree(module.getDependencies(), 2);
                }

                return pom;

            } catch (Exception e) {
                LOGGER.error("erreur pour récupérer l'effective pom de {}", repertoire, e);
            }
        }
        return null;
    }

    private void printDependencyTree(List<MavenDependency> deps, int indent) {
        for (MavenDependency dep : deps) {
            LOGGER.info("{}├─ {}", " ".repeat(indent), dep);
            printDependencyTree(dep.getTransitiveDependencies(), indent + 3);
        }
    }

}
