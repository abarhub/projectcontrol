package org.projectcontrol.core.service;

import com.google.common.base.Verify;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.projectcontrol.core.utils.LigneAModifier;
import org.projectcontrol.core.vo.ResultatBalise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PomParserService {


    private static final Logger LOGGER = LoggerFactory.getLogger(PomParserService.class);

    public static final List<String> PROJET_VERSION = List.of("project", "version");
    public static final List<String> PROJET_GROUPEID = List.of("project", "groupId");
    public static final List<String> PROJET_ARTIFACTID = List.of("project", "artifactId");
    public static final List<String> PROJET_PARENT_VERSION = List.of("project", "parent", "version");
    public static final List<String> PROJET_PARENT_GROUPEID = List.of("project", "parent", "groupId");
    public static final List<String> PROJET_PARENT_ARTIFACTID = List.of("project", "parent", "artifactId");

    private final XmlParserService xmlParserService;

    public PomParserService(XmlParserService xmlParserService) {
        this.xmlParserService = xmlParserService;
    }

    public void parsePom(Path file) throws Exception {
        var resultat = xmlParserService.parse(file, List.of(PROJET_VERSION, PROJET_ARTIFACTID, PROJET_GROUPEID));
    }

    public void updateVersion(final Path file, final String version, boolean commit, String messageCommit) throws Exception {
        var resultat = xmlParserService.parse(file, List.of(PROJET_VERSION, PROJET_ARTIFACTID, PROJET_GROUPEID));
        if (!resultat.isEmpty()) {
            var versionOpt = resultat.stream().filter(x -> Objects.equals(x.balises(), PROJET_VERSION)).findFirst();
            if (versionOpt.isPresent()) {
                var versionOld = versionOpt.get();
                LOGGER.info("version: {} <> {} (fichier: {})", versionOld.valeur(), version, file);
                xmlParserService.modifierFichier2(file.toString(), versionOld.positionDebut(), versionOld.positionFin(), version);
                var repoDir = file.getParent().resolve(".git");
                if (commit && Files.exists(repoDir)) {
                    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
                    Repository repository = repositoryBuilder.setGitDir(repoDir.toFile())
                            .readEnvironment() // Lire GIT_DIR et d'autres variables d'environnement
                            .findGitDir() // Chercher le répertoire .git
                            .build();
                    try (Git git = new Git(repository)) {

                        AddCommand add = git.add();
                        add.addFilepattern(file.getFileName().toString()).call();

                        updateEnfantPom(resultat, file, version, git);

                        String message = "chore(version): préparation de la version " + version;
                        if (messageCommit != null) {
                            message = messageCommit;
                        }
                        git.commit().setMessage(message).call();
                    }
                } else {
                    updateEnfantPom(resultat, file, version, null);
                }
            }
        }
    }

    private void updateEnfantPom(List<ResultatBalise> resultat, final Path file, final String version, Git git) throws Exception {
        var groupIdOpt = resultat.stream()
                .filter(x -> Objects.equals(x.balises(), PROJET_GROUPEID))
                .findFirst();
        var artifactIdOpt = resultat.stream()
                .filter(x -> Objects.equals(x.balises(), PROJET_ARTIFACTID))
                .findFirst();
        if (groupIdOpt.isPresent() && artifactIdOpt.isPresent()) {
            var groupId = groupIdOpt.get().valeur();
            var artifactId = artifactIdOpt.get().valeur();
            if (StringUtils.isNotBlank(groupId) || StringUtils.isNotBlank(artifactId)) {
                try (var stream = Files.list(file.getParent())) {
                    List<Path> liste2 = stream
                            .filter(Files::isDirectory)
                            .filter(x -> Files.exists(x.resolve("pom.xml")))
                            .toList();
                    LOGGER.info("liste2: {}", liste2);
                    for (var path : liste2) {
                        var file2 = path.resolve("pom.xml");
                        var resultat2 = xmlParserService.parse(file2, List.of(PROJET_PARENT_VERSION,
                                PROJET_PARENT_GROUPEID, PROJET_PARENT_ARTIFACTID));
                        if (!resultat2.isEmpty()) {
                            var groupIdOpt2 = resultat2.stream()
                                    .filter(x -> Objects.equals(x.balises(), PROJET_PARENT_GROUPEID))
                                    .findFirst();
                            var artifactIdOpt2 = resultat2.stream()
                                    .filter(x -> Objects.equals(x.balises(), PROJET_PARENT_ARTIFACTID))
                                    .findFirst();
                            var versionOpt2 = resultat2.stream().filter(x -> Objects.equals(x.balises(), PROJET_PARENT_VERSION)).findFirst();
                            if (groupIdOpt2.isPresent() && artifactIdOpt2.isPresent() && versionOpt2.isPresent()) {
                                var groupParentId = groupIdOpt2.get().valeur();
                                var artifactParentId = artifactIdOpt2.get().valeur();
                                var versionParent = versionOpt2.get();
                                if (Objects.equals(groupId, groupParentId) && Objects.equals(artifactId, artifactParentId)) {
                                    LOGGER.info("version parent: {} <> {} (fichier: {})", versionParent.valeur(), version, file2);
                                    xmlParserService.modifierFichier(file2.toString(), versionParent.positionDebut(),
                                            versionParent.positionFin(), version);
                                    if (git != null) {
                                        AddCommand add = git.add();
                                        add.addFilepattern(path.getFileName() + "/" + file2.getFileName().toString()).call();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void updateVersion2(Path file, String versionInitiale, boolean commit, String messageCommit,
                               Map<String, List<LigneAModifier>> listLignes, List<String> listeIdLignes, String versionModifiee) throws IOException {
        LOGGER.info("version: {} <> {} (fichier: {})", versionInitiale, versionModifiee, file);
        LOGGER.info("liste: {}", listLignes);
        Path repRacine = file.getParent();
        for (var entry : listLignes.entrySet()) {
            Path file2 = repRacine.resolve(entry.getKey());
            LOGGER.info("fichier: {}", file2);
            var contenu = Files.readAllLines(file2);
            var modification = false;
            for (var ligne : entry.getValue()) {
                LOGGER.info("ligne: {}", ligne);
                var pos = ligne.noLigne();
                var s = contenu.get(pos);
                Verify.verify(CollectionUtils.size(ligne.positionModification()) == 1, "il y a plusieurs modifications dans la ligne");
                var inter = ligne.positionModification().getFirst();
                var debut = s.substring(0, inter.debut());
                var fin = s.substring(inter.fin() + 1);
                var s2 = debut + versionModifiee + fin;
                contenu.set(pos, s2);
                modification = true;
            }
            if (modification) {
                Files.write(file2, contenu);
            }
        }
    }
}
