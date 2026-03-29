package org.projectcontrol.core.service;

import org.projectcontrol.core.vo.projet.DetailProjet;
import org.projectcontrol.core.vo.projet.GitRepositoryInfo;
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

    private final GitInfoService gitInfoService;

    public ProjetDetailService(EffectivePomReaderService reader, NodeReaderService nodeReaderService, GitInfoService gitInfoService) {
        this.reader = reader;
        this.nodeReaderService = nodeReaderService;
        this.gitInfoService = gitInfoService;
    }

    public DetailProjet getProjetDetail(Path repertoire) {
        MavenProjet mavenProjet = analysePom(repertoire);

        List<NodeProjet> nodeProjets = nodeReaderService.analyse(repertoire);

        GitRepositoryInfo gitRepositoryInfo = analyseGit(repertoire);

        return new DetailProjet(mavenProjet, nodeProjets, gitRepositoryInfo);
    }

    private GitRepositoryInfo analyseGit(Path repertoire) {
        try {
            return gitInfoService.analyzeRepository(repertoire.toString());
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'analyse du git de {}", repertoire, e);
            return null;
        }
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
