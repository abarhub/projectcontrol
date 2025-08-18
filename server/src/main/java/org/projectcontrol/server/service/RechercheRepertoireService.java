package org.projectcontrol.server.service;

import org.projectcontrol.server.enumeration.ModuleProjetEnum;
import org.projectcontrol.server.vo.Projet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class RechercheRepertoireService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RechercheRepertoireService.class);

    private static final Set<String> SET_DIR = Set.of("target", "node_modules", "venv", ".metadata", ".git");

    public static final String POM_XML = "pom.xml";
    public static final String PACKAGE_JSON = "package.json";
    public static final String GO_MOD = "go.mod";
    public static final String CARGO_TOML = "Cargo.toml";
    private static final Set<String> FICHIER_PROJET = Set.of(POM_XML, PACKAGE_JSON, GO_MOD, CARGO_TOML);

    public List<Projet> findPomFiles(Path startDir, Set<String> directoriesExclude) throws IOException {
        List<Projet> pomFiles = new ArrayList<>();

        Set<String> directoriesExclude2 = new HashSet<>(SET_DIR);
        if (directoriesExclude != null && !directoriesExclude.isEmpty() && directoriesExclude.size() > 0) {
            directoriesExclude2.addAll(directoriesExclude);
        }

        Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName().toString();
                // Ignorer les répertoires node_modules et target
                if (Files.isDirectory(dir) && (directoriesExclude2.contains(dirName))) {
                    LOGGER.debug("preVisitDirectory: skip dir");
                    return FileVisitResult.SKIP_SUBTREE; // Ne pas visiter ce répertoire ni ses sous-répertoires
                }
                for (var nom : FICHIER_PROJET) {
                    if (Files.exists(dir.resolve(nom))) {
                        Path file = dir.resolve(nom);
                        Projet projet = new Projet();
                        projet.setNom(file.getParent().getFileName().toString());
                        projet.setRepertoire(file.getParent().toAbsolutePath().toString());
                        if (Objects.equals(nom, POM_XML)) {
                            projet.setFichierPom(file.toAbsolutePath().toString());
                        }
                        try {
                            FileTime dateModif = Files.getLastModifiedTime(dir);
                            if (dateModif != null) {
                                var date = dateModif.toInstant();
                                projet.setDateModification(LocalDateTime.ofInstant(date, ZoneId.systemDefault()));
                            }
                        } catch (IOException e) {
                            LOGGER.error("Erreur lors de la lecture du fichier {} : {}", nom, e.getMessage(), e);
                        }
                        completeProjet(dir, projet);
                        pomFiles.add(projet);
                        LOGGER.debug("preVisitDirectory: skip from project file");
                        //return FileVisitResult.SKIP_SIBLINGS;
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE; // Continuer la visite
            }

//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                LOGGER.info("visiteFile: {}", file.toString());
//                if (Objects.equals(file.getFileName().toString(), "pom.xml")) {
//                    Projet projet = new Projet();
//                    projet.setNom(file.getParent().getFileName().toString());
//                    projet.setRepertoire(file.getParent().toAbsolutePath().toString());
//                    projet.setFichierPom(file.toAbsolutePath().toString());
//                    pomFiles.add(projet);
//                    // Si un pom.xml est trouvé, nous ne voulons pas regarder dans les sous-répertoires
//                    // Ici, cela signifie qu'on a trouvé un pom.xml dans le répertoire courant,
//                    // donc on peut sauter les autres fichiers dans ce même répertoire,
//                    // mais cela ne signifie pas que l'on doit arrêter de chercher ailleurs
//                    // car le SimpleFileVisitor parcourt l'arbre de manière hiérarchique.
//                    // L'arrêt de la recherche dans les sous-répertoires est géré par la logique
//                    // de `postVisitDirectory` ou par l'idée que `pom.xml` est souvent à la racine d'un module.
//                    // Pour le cas "Si je trouve un fichier pom, je ne veux pas regarder dans les sous répertoire",
//                    // cela s'applique plus à une structure de module où un pom.xml marque le début d'un module.
//                    // Si le pom.xml est trouvé dans un dossier, on considèrera que ce dossier est un module
//                    // et on ne cherchera pas de pom.xml dans les sous-dossiers de ce même module.
//                    LOGGER.debug("visiteFile: skip");

            ////                    return FileVisitResult.SKIP_SUBTREE;
//                    return FileVisitResult.SKIP_SIBLINGS;
//                }
//                return FileVisitResult.CONTINUE; // Continuer la visite
//            }

//            @Override
//            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//                // Cette partie est cruciale pour l'exigence "Si je trouve un fichier pom, je ne veux pas regarder dans les sous répertoire."
//                // Si un pom.xml a été ajouté à la liste et que le répertoire courant contient ce pom.xml,
//                // alors nous voulons "sauter" la suite de ce répertoire (si d'autres fichiers ou sous-dossiers étaient encore à visiter dans ce même dossier après le pom.xml).
//                // Cependant, le `SimpleFileVisitor` visite d'abord les fichiers et ensuite seulement appelle `postVisitDirectory`.
//                // L'idée est plutôt que si `dir` est un répertoire et qu'il contient un `pom.xml`, nous avons déjà trouvé ce `pom.xml` dans `visitFile`.
//                // Ce que l'on veut, c'est que si `dir` est un module Maven (identifié par un pom.xml à sa racine),
//                // alors on ne descende pas dans les sous-répertoires de `dir` pour chercher d'autres `pom.xml`.
//                // Le `preVisitDirectory` gère déjà l'exclusion des répertoires `node_modules` et `target`.
//                // Pour l'exigence "Si je trouve un fichier pom, je ne veux pas regarder dans les sous répertoire",
//                // la meilleure façon de l'implémenter est de ne pas visiter les sous-répertoires d'un répertoire qui *contient* un pom.xml.
//                // Cela signifie qu'il faut vérifier l'existence de `pom.xml` dans `preVisitDirectory` pour décider si on continue de descendre.
//
//                // Pour implémenter "Si je trouve un fichier pom, je ne veux pas regarder dans les sous répertoire",
//                // nous devons modifier `preVisitDirectory` pour vérifier l'existence de `pom.xml` dans le répertoire courant.
//                // Cela est un peu plus complexe car `preVisitDirectory` est appelé avant que nous ayons visité les fichiers du répertoire courant.
//                // Une approche serait de maintenir un ensemble de répertoires déjà traités ou de modifier la logique de `preVisitDirectory`.
//                // La solution la plus simple qui respecte l'esprit "trouver le pom.xml le plus haut dans la hiérarchie pour un module"
//                // est de s'assurer que si un `pom.xml` est trouvé dans un répertoire, on n'ajoute pas les `pom.xml` des sous-répertoires de ce même répertoire.
//                // La liste `pomFiles` contient déjà les chemins complets, donc si nous trouvons `A/pom.xml` et `A/B/pom.xml`, les deux seront ajoutés.
//                // Pour respecter l'exigence, il faudrait post-filtrer ou adapter la logique de visite.
//
//                // Refactorisons pour l'exigence "Si je trouve un fichier pom, je ne veux pas regarder dans les sous répertoire."
//                // Cela signifie que si `dir` contient un `pom.xml`, nous ne voulons pas continuer à chercher dans les sous-dossiers de `dir`.
//                // Le `SKIP_SUBTREE` dans `preVisitDirectory` est la clé pour cela.
//                // Nous devons donc détecter la présence d'un `pom.xml` avant d'entrer dans les sous-répertoires.
//                // La manière la plus propre serait de vérifier si le répertoire contient un `pom.xml`
//                // *avant* d'appeler `CONTINUE` dans `preVisitDirectory`.
//
//                return FileVisitResult.CONTINUE; // Continuer
//            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                LOGGER.error("Erreur lors de la visite du fichier {}: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE; // Continuer même en cas d'erreur sur un fichier
            }
        });
        return pomFiles;
    }

    public void completeProjet(Path dir, Projet projet) {
        if (Files.exists(dir.resolve(POM_XML))) {
            var file = dir.resolve(POM_XML);
            projet.setFichierPom(file.toAbsolutePath().toString());
            ajouteModule(projet, ModuleProjetEnum.POM);
        }
        if (Files.exists(dir.resolve(PACKAGE_JSON))) {
            var file = dir.resolve(PACKAGE_JSON);
            projet.setPackageJson(file.toAbsolutePath().toString());
            ajouteModule(projet, ModuleProjetEnum.NODEJS);
        }
        if (Files.exists(dir.resolve(GO_MOD))) {
            var file = dir.resolve(GO_MOD);
            projet.setGoMod(file.toAbsolutePath().toString());
            ajouteModule(projet, ModuleProjetEnum.GO);
        }
        if (Files.exists(dir.resolve(CARGO_TOML))) {
            var file = dir.resolve(CARGO_TOML);
            projet.setCargoToml(file.toAbsolutePath().toString());
            ajouteModule(projet, ModuleProjetEnum.RUST);
        }
    }

    private void ajouteModule(Projet projet, ModuleProjetEnum moduleProjetEnum) {
        if (projet.getModules() == null) {
            projet.setModules(new HashSet<>());
        }
        projet.getModules().add(moduleProjetEnum);
    }
}
