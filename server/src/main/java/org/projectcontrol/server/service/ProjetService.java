package org.projectcontrol.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.projectcontrol.core.service.PomParserService;
import org.projectcontrol.core.service.XmlParserService;
import org.projectcontrol.server.dto.ArtifactDto;
import org.projectcontrol.server.dto.GroupeProjetDto;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.mapper.ProjetMapper;
import org.projectcontrol.server.properties.ApplicationProperties;
import org.projectcontrol.server.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toMap;

@Service
public class ProjetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjetService.class);

    private static final Set<String> SET_DIR = Set.of("target", "node_modules", "venv", ".metadata", ".git");

    public static final String POM_XML = "pom.xml";
    public static final String PACKAGE_JSON = "package.json";
    public static final String GO_MOD = "go.mod";
    public static final String CARGO_TOML = "Cargo.toml";
    private static final Set<String> FICHIER_PROJET = Set.of(POM_XML, PACKAGE_JSON, GO_MOD, CARGO_TOML);

    @Value("${repertoireProjet:}")
    private String repertoireProjet;

    @Autowired
    private XmlParserService XmlParserService;

    @Autowired
    private PomParserService pomParserService;

    private final ApplicationProperties applicationProperties;

    private final ProjetMapper projetMapper;

    public ProjetService(ApplicationProperties applicationProperties, ProjetMapper projetMapper) {
        this.projetMapper = projetMapper;
        LOGGER.info("creation repertoireProjet: {}", repertoireProjet);
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("init repertoireProjet: {}", repertoireProjet);
    }

    public List<Projet> getProjets(String directoryPath) {
        LOGGER.info("répertoire: {}", directoryPath);
        if (directoryPath == null || directoryPath.isEmpty()) {
            throw new RuntimeException("Répertoire vide");
        }
        return listePom(directoryPath);
    }

    public List<Projet> getProjets() {
        String rep = this.applicationProperties.getListeProjets()
                .entrySet()
                .stream()
                .filter(x -> x.getValue().isDefaut())
                .map(x -> x.getValue().getRepertoires().getFirst())
                .findAny().orElse(null);
        LOGGER.info("répertoire: {}", rep);
        if (rep == null || rep.isEmpty()) {
            throw new RuntimeException("Répertoire vide");
        }
        return listePom(rep);
    }


    public List<Projet> getProjets2(String groupId) {
        var groupeOpt = this.applicationProperties.getListeProjets()
                .entrySet()
                .stream()
                .filter(x -> Objects.equals(x.getKey(), groupId))
                .findAny();
        if (groupeOpt.isEmpty()) {
            throw new RuntimeException("Impossible de trouver le groupe " + groupId);
        }
        var groupe = groupeOpt.get();
        List<String> rep = List.of();
        if (!CollectionUtils.isEmpty(groupe.getValue().getRepertoires())) {
            rep = groupe.getValue().getRepertoires();
        }
        LOGGER.info("répertoire: {}", rep);
        if (rep == null || rep.isEmpty()) {
            throw new RuntimeException("Répertoire vide");
        }

        Set<String> directoriesExclude = null;
        if (groupe.getValue().getExclusions() != null && !groupe.getValue().getExclusions().isEmpty()) {
            directoriesExclude = new HashSet<>(groupe.getValue().getExclusions());
        }

        return listePom(rep, directoriesExclude);
    }

    private List<Projet> listePom(String directoryPath) {
        return listePom(List.of(directoryPath), null);
    }

    private List<Projet> listePom(List<String> directoryPath, Set<String> directoriesExclude) {
        //String directoryPath = repertoireProjet; // Remplacez par le chemin de votre répertoire
//        Path p = Paths.get(directoryPath).toAbsolutePath().normalize();


        try {
//            List<Path> pomFiles = findPomFiles(directoryPath);
            LOGGER.info("récupération des fichiers pom ...");
            List<Projet> pomFiles = new ArrayList<>();
            for (String path : directoryPath) {
                Path p = Paths.get(path).toAbsolutePath().normalize();
                List<Projet> pomFiles2 = findPomFiles(p, directoriesExclude);
                for (var pom : pomFiles2) {
                    if (pomFiles.stream()
                            .noneMatch(x -> Objects.equals(x.getFichierPom(), pom.getFichierPom()))) {
                        pomFiles.add(pom);
                    }
                }
            }

            LOGGER.info("récupération des fichiers pom ok");
            if (pomFiles.isEmpty()) {
                LOGGER.info("Aucun fichier pom.xml trouvé (en ignorant target et node_modules) dans : {}", directoryPath);
            } else {
                LOGGER.info("Fichiers pom.xml trouvés (en ignorant target et node_modules) :");
                for (Projet pomFile : pomFiles) {
                    LOGGER.info("{}", pomFile.getFichierPom());
                }
            }
            return pomFiles;
        } catch (IOException e) {
            LOGGER.error("Erreur lors du parcours du répertoire : {}", e.getMessage(), e);
        }
        return null;
    }

    public static List<Projet> findPomFiles(Path startDir, Set<String> directoriesExclude) throws IOException {
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

    private static void completeProjet(Path dir, Projet projet) {
        if (Files.exists(dir.resolve(POM_XML))) {
            var file = dir.resolve(POM_XML);
            projet.setFichierPom(file.toAbsolutePath().toString());
        }
        if (Files.exists(dir.resolve(PACKAGE_JSON))) {
            var file = dir.resolve(PACKAGE_JSON);
            projet.setPackageJson(file.toAbsolutePath().toString());
        }
        if (Files.exists(dir.resolve(GO_MOD))) {
            var file = dir.resolve(GO_MOD);
            projet.setGoMod(file.toAbsolutePath().toString());
        }
        if (Files.exists(dir.resolve(CARGO_TOML))) {
            var file = dir.resolve(CARGO_TOML);
            projet.setCargoToml(file.toAbsolutePath().toString());
        }
    }

//    public static List<Path> findPomFiles(String directoryPath) throws IOException {
//        Path startPath = Paths.get(directoryPath);
//
//        try (Stream<Path> walk = Files.walk(startPath)) {
//            return walk
//                    .filter(Files::isRegularFile) // Ne traiter que les fichiers réguliers
//                    .filter(path -> path.getFileName().toString().equals("pom.xml")) // Chercher les fichiers nommés pom.xml
//                    .filter(ProjetService::isNotIgnoredDirectory) // Ignorer les répertoires spécifiques
//                    .collect(Collectors.toList());
//        }
//    }

//    private static boolean isNotIgnoredDirectory(Path path) {
//        // Vérifier si le chemin contient "target" ou "node_modules" comme nom de répertoire
//        // Cela permet de s'assurer que même si un pom.xml se trouve dans un sous-sous-répertoire d'un répertoire ignoré, il est bien ignoré.
//        for (Path segment : path) {
//            String segmentName = segment.getFileName().toString();
//            if (segmentName.equals("target") || segmentName.equals("node_modules")) {
//                return false; // Le chemin contient un répertoire à ignorer
//            }
//        }
//        return true; // Le chemin ne contient pas de répertoire à ignorer
//    }

//    public void updateProject(Projet selectedProduct) throws Exception {
//        updateProject4(selectedProduct);
//    }


//    private void afficheVersion(Path inputFile, Position debut, Position fin, String versionActuelle) {
//        List<String> listeVersions = getListeVersion(versionActuelle);
//        List<String> liste2 = new ArrayList<>();
//        Map<String, String> map = new HashMap<>();
//        for (String version : listeVersions) {
//            var libelle = "Version " + version;
//            map.put(libelle, version);
//            liste2.add(libelle);
//        }
//        final String autre = "Autre (saisir)";
//        liste2.add(autre);
//        Stage primaryStage = new Stage();
//        primaryStage.setTitle("Sélectionnez la nouvelle version (version actuelle : " + versionActuelle + ")");
//        // 1. Création de la ComboBox
//        var comboBox = new ComboBox<String>();
//        comboBox.setItems(FXCollections.observableArrayList(liste2));
//        comboBox.setPromptText("Sélectionnez une option"); // Texte par défaut
//
//        // 2. Création du TextField (initialement masqué)
//        var textField = new TextField();
//        textField.setPromptText("Saisissez votre valeur");
//        textField.setVisible(false); // Masqué par défaut
//        textField.setManaged(false); // N'affecte pas l'agencement quand il est masqué
//
//        // 3. Gestion de l'affichage/masquage du TextField en fonction de la sélection de la ComboBox
//        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
//            if (autre.equals(newValue)) {
//                textField.setVisible(true);
//                textField.setManaged(true);
//            } else {
//                textField.setVisible(false);
//                textField.setManaged(false);
//                textField.clear(); // Efface le contenu si l'option "Autre" n'est plus sélectionnée
//            }
//        });
//
//        // 4. Création du bouton de validation
//        var validateButton = new Button("Valider");
//        var messageLabel = new Label(); // Initialisation du label pour les messages
//
//        // 5. Action du bouton de validation
//        validateButton.setOnAction(event -> {
//            String selectedOption = comboBox.getSelectionModel().getSelectedItem();
//            String message = "";
//
//            if (selectedOption == null) {
//                message = "Veuillez sélectionner une option.";
//            } else if (autre.equals(selectedOption)) {
//                if (textField.getText().isEmpty()) {
//                    message = "Veuillez saisir une valeur pour 'Autre'.";
//                } else {
//                    message = "Option sélectionnée : Autre, Valeur saisie : " + textField.getText();
//                    var version = textField.getText();
//                    if (StringUtils.isNotBlank(version)) {
//                        try {
//                            pomParserService.updateVersion(inputFile, version);

    /// /                            this.XmlParserService.modifierFichier(inputFile.toString(), debut, fin, version);
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }
//            } else {
//                var version = map.get(selectedOption);
//                message = "Option sélectionnée : " + selectedOption + " (" + version + ")";
//                if (StringUtils.isNotBlank(version)) {
//                    try {
//                        //this.XmlParserService.modifierFichier(inputFile.toString(), debut, fin, version);
//                        pomParserService.updateVersion(inputFile, version);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//            messageLabel.setText(message);
//        });
//
//        // 6. Agencement des éléments dans un VBox
//        VBox root = new VBox(10); // Espacement de 10 pixels entre les enfants
//        root.setPadding(new Insets(20)); // Marge intérieure de 20 pixels
//        root.setAlignment(Pos.TOP_CENTER); // Alignement des éléments au centre en haut
//
//        root.getChildren().addAll(comboBox, textField, validateButton, messageLabel);
//
//        // 7. Création de la scène et affichage de la fenêtre
//        Scene scene = new Scene(root, 600, 300); // Largeur, Hauteur
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }
    private List<String> getListeVersion(String versionActuelle) {
        List<String> listeVersions = new ArrayList<>();
        String snapshotSuffix = "-SNAPSHOT";
        if (versionActuelle.endsWith(snapshotSuffix)) {
            versionActuelle = versionActuelle.substring(0, versionActuelle.length() - snapshotSuffix.length());
        }
        String[] versions = versionActuelle.split("\\.");
        List<Integer> listeVersionsInt = new ArrayList<>();
        for (String version : versions) {
            listeVersionsInt.add(Integer.parseInt(version));
        }
        for (int i = 0; i < listeVersionsInt.size(); i++) {
            StringBuilder s = new StringBuilder();
            for (int j = 0; j < listeVersionsInt.size(); j++) {
                if (!s.isEmpty()) {
                    s.append(".");
                }
                var n = listeVersionsInt.get(j);
                if (j == i) {
                    n++;
                } else if (j > i) {
                    n = 0;
                }
                s.append(n);
            }
            s.append(snapshotSuffix);
            listeVersions.add(s.toString());
        }
        return listeVersions;
    }

    public List<ProjetDto> getProjetDto(String groupId, String nomProjet) {
        var liste = getProjets2(groupId);
        if (liste != null && !liste.isEmpty() && StringUtils.isNotBlank(nomProjet) && liste.stream().anyMatch(p -> p.getNom().equals(nomProjet))) {
            liste = liste.stream().filter(p -> p.getNom().equals(nomProjet)).toList();
        }
        List<ProjetDto> listeResultat = new ArrayList<>();
        for (Projet projet : liste) {
            LOGGER.info("Analyse du projet {}", projet.getNom());
            try {
                var projetDto = new ProjetDto();
                analyseProjet(projet);
                projetDto.setNom(projet.getNom());
                projetDto.setDescription(projet.getDescription());
                projetDto.setFichierPom(projet.getFichierPom());
                projetDto.setPackageJson(projet.getPackageJson());
                projetDto.setGoMod(projet.getGoMod());
                projetDto.setCargoToml(projet.getCargoToml());
                projetDto.setRepertoire(projet.getRepertoire());
                if (projet.getProjetPom() != null) {
                    var pom = projet.getProjetPom();
                    copiePom(pom, projetDto);
                }
                this.projetMapper.projetToProjetDto(projet,projetDto);
                listeResultat.add(projetDto);
            } catch (Exception e) {
                LOGGER.error("Erreur lors de l'analyse du projet {}", projet.getNom(), e);
            }
        }
        return listeResultat;
    }

    private List<ProjetDto> convert(List<ProjetPom> projetPomEnfants) {
        List<ProjetDto> projetEnfants;
        projetEnfants = new ArrayList<>();
        for (ProjetPom pom : projetPomEnfants) {
            ProjetDto projet = new ProjetDto();
            projet.setNom(pom.getNom());
            copiePom(pom, projet);
            projetEnfants.add(projet);
        }

        return projetEnfants;
    }

    private void copiePom(ProjetPom pom, ProjetDto projet) {
        if (pom.getParent() != null) {
            projet.setParent(convert(pom.getParent()));
        }
        if (pom.getArtifact() != null) {
            projet.setArtifact(convert(pom.getArtifact()));
        }
        if (pom.getProjetPomEnfants() != null) {
            projet.setProjetEnfants(convert(pom.getProjetPomEnfants()));
        }
        if (pom.getProperties() != null) {
            projet.setProperties(pom.getProperties());
        }
        if (pom.getDependencies() != null) {
            projet.setDependencies(pom.getDependencies());
        }
    }

    private ArtifactDto convert(ArtefactMaven artifact) {
        var artifactDto = new ArtifactDto();
        artifactDto.setGroupId(artifact.groupId());
        artifactDto.setArtefactId(artifact.artefactId());
        artifactDto.setVersion(artifact.version());
        return artifactDto;
    }

    private void analyseProjet(Projet projet) throws IOException {
        if (StringUtils.isNotBlank(projet.getFichierPom())) {
            Path pomFile = Path.of(projet.getFichierPom());

            analysePom(pomFile, projet);
        }

        if (StringUtils.isNotBlank(projet.getPackageJson())) {
            Path jsonFile = Path.of(projet.getPackageJson());

            ProjetNode resultat = new ProjetNode();
            analysePackageJson(jsonFile, resultat);
            projet.setProjetNode(resultat);
        }

//        if (StringUtils.isNotBlank(projet.getGoMod())) {
//            analyseGoMod(projet.getGoMod(), resultat);
//        }

//        if (StringUtils.isNotBlank(projet.getCargoToml())) {
//            analyseCargo(projet.getCargoToml(), resultat);
//        }
        Path pathGit = Path.of(projet.getRepertoire()).resolve(".git");
        if (Files.exists(pathGit)) {
            ProjetGit resultat = new ProjetGit();
            analyseGit(pathGit, resultat);
            projet.setProjetGit(resultat);
        }
    }

    private void analyseGit(Path pathGit, ProjetGit resultat) {
        try (Repository repository = new RepositoryBuilder().setGitDir(pathGit.toFile()).readEnvironment().findGitDir().build()) {

            RevCommit latestCommit = new Git(repository).
                    log().
                    setMaxCount(1).
                    call().
                    iterator().
                    next();

            String latestCommitHash = latestCommit.getName();
            resultat.setIdCommitComplet(latestCommitHash);
            resultat.setIdCommit(latestCommitHash.substring(0, 7));
            resultat.setMessage(latestCommit.getFullMessage());
            resultat.setBranche(repository.getFullBranch());
            var date = Instant.ofEpochSecond(latestCommit.getCommitTime());
            resultat.setDate(LocalDateTime.from(date));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'analyse du projet {}", pathGit, e);
        }
    }

    private void analyseCargo(String cargoToml, StringBuilder resultat) throws IOException {
        if (StringUtils.isNotBlank(cargoToml)) {
            Path path = Paths.get(cargoToml);
            if (Files.exists(path)) {
                resultat.append("fichier cargo ").append(cargoToml).append("\n");
                List<String> liste = Files.readAllLines(path);
                for (String line : liste) {
                    resultat.append(line).append("\n");
                }
            }
        }
    }

    private void analyseGoMod(String goMod, StringBuilder resultat) throws IOException {
        if (StringUtils.isNotBlank(goMod)) {
            Path path = Paths.get(goMod);
            if (Files.exists(path)) {
                resultat.append("fichier go projet ").append(goMod).append("\n");
                List<String> liste = Files.readAllLines(path);
                for (String line : liste) {
                    resultat.append(line).append("\n");
                }
            }
        }
    }

    private void analysePackageJson(Path jsonFile, ProjetNode resultat) throws IOException {

        if (Files.exists(jsonFile)) {

            ObjectMapper mapper = new ObjectMapper();

            try (var reader = Files.newBufferedReader(jsonFile)) {
                JsonNode node = mapper.reader().readTree(reader);

//                resultat.append("fichier ").append(jsonFile).append("\n");
                if (node.has("name")) {
//                    resultat.append("nom:").append(node.get("name")).append("\n");
                    resultat.setNom(node.get("name").asText());
                }
                if (node.has("version")) {
//                    resultat.append("version:").append(node.get("version")).append("\n");
                    resultat.setVersion(node.get("version").asText());
                }
                if (node.has("scripts")) {
//                    resultat.append("script:").append("\n");
//                    List<String> liste = new ArrayList<>();
                    Map<String, String> map = new TreeMap<>();
                    for (var script : node.get("scripts").properties()) {
//                        liste.add(script.getKey() + ":" + script.getValue().toString());
                        map.put(script.getKey(), script.getValue().toString());
                    }
//                    Collections.sort(liste);
//                    liste.forEach(x -> resultat.append("\t").append(x).append("\n"));
                    resultat.setScript(map);
                }
                if (node.has("dependencies")) {
//                    resultat.append("dependencies:").append("\n");
//                    List<String> liste = new ArrayList<>();
                    Map<String, String> map = new TreeMap<>();
                    for (var script : node.get("dependencies").properties()) {
//                        liste.add(script.getKey() + ":" + script.getValue().toString());
                        map.put(script.getKey(), script.getValue().toString());
                    }
//                    Collections.sort(liste);
//                    liste.forEach(x -> resultat.append("\t").append(x).append("\n"));
                    resultat.setDependencies(map);
                }
                if (node.has("devDependencies")) {
//                    resultat.append("devDependencies:").append("\n");
//                    List<String> liste = new ArrayList<>();
                    Map<String, String> map = new TreeMap<>();
                    for (var script : node.get("devDependencies").properties()) {
//                        liste.add(script.getKey() + ":" + script.getValue().toString());
                        map.put(script.getKey(), script.getValue().toString());
                    }
//                    Collections.sort(liste);
//                    liste.forEach(x -> resultat.append("\t").append(x).append("\n"));
                    resultat.setDevDependencies(map);
                }
            }

        }

    }

    private void analysePom(Path pomFile, Projet projet) throws IOException {
        if (pomFile != null) {
            ProjetPom projetPom = new ProjetPom();
            if (Files.exists(pomFile)) {

                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model model = null;
                try (var fileReader = Files.newBufferedReader(pomFile)) {
                    model = reader.read(fileReader);
                } catch (Exception e) {
                    // Gérer les exceptions de lecture XML (par exemple, fichier mal formé)
                    throw new IOException("Erreur lors de la lecture du fichier POM : " + pomFile, e);
                }
                projet.setProjetPom(projetPom);

                if (model != null) {
                    projet.setDescription(model.getDescription());
                    if (model.getName() != null) {
                        projetPom.setNom(model.getName());
                    }
                    Parent parent = model.getParent();
                    if (parent != null) {
//                        String s = parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion();
//                        resultat.append("parent:").append(s).append("\n");
                        ArtefactMaven parentArtefact = new ArtefactMaven(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                        projetPom.setParent(parentArtefact);

                    } else {
                        //resultat.append("parent:").append("\n");
                    }
//                    String s = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
                    //resultat.append("version:").append(s).append("\n");
                    ArtefactMaven artefactMaven = new ArtefactMaven(model.getGroupId(), model.getArtifactId(), model.getVersion());
                    projetPom.setArtifact(artefactMaven);

                    if (!CollectionUtils.isEmpty(model.getProperties())) {
//                        resultat.append("properties:\n");
//                        List<String> liste = new ArrayList<>();
                        Map<String, String> map = new TreeMap<>();
                        model.getProperties().forEach((nom, valeur) -> {
                            //liste.add(nom + ":" + valeur);
                            if (valeur != null) {
                                map.put((String) nom, valeur.toString());
                            } else {
                                map.put((String) nom, "");
                            }
                        });
                        projetPom.setProperties(map);
//                        Collections.sort(liste);
//                        liste.forEach(x -> {
//                            resultat.append("\t").append(x).append("\n");
//                        });
                    }
                    if (!CollectionUtils.isEmpty(model.getDependencies())) {
//                        resultat.append("dependencies:\n");
                        List<ArtefactMaven> liste = new ArrayList<>();
                        model.getDependencies().forEach((dep) -> {
//                            String s2 = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
                            liste.add(new ArtefactMaven(dep.getGroupId(), dep.getArtifactId(), dep.getVersion()));
                        });
                        projetPom.setDependencies(liste);
//                        Collections.sort(liste);
//                        liste.forEach(x -> {
//                            resultat.append("\t").append(x).append("\n");
//                        });

                    }
                }

                // analyse des enfants

                var liste = Files.list(pomFile.getParent())
                        .filter(Files::isDirectory)
                        .toList();

                for (var f : liste) {
                    var f2 = f.resolve("pom.xml");
                    if (Files.exists(f2)) {
//                        resultat.append("* enfant ").append(f.getFileName()).append(" :").append("\n");
                        Projet projetEnfant = new Projet();
                        projetEnfant.setNom(f.getFileName().toString());
                        analysePom(f2, projetEnfant);
                        if (projetEnfant.getProjetPom() != null) {
                            if (projetPom.getProjetPomEnfants() == null) {
                                projetPom.setProjetPomEnfants(new ArrayList<>());
                            }
                            if (projetEnfant.getNom() != null && projetEnfant.getProjetPom().getNom() == null) {
                                projetEnfant.getProjetPom().setNom(projetEnfant.getNom());
                            }
                            projetPom.getProjetPomEnfants().add(projetEnfant.getProjetPom());
                        }

//
                        var f3 = f.resolve("package.json");
                        if (Files.exists(f3)) {
                            ProjetNode resultat2 = new ProjetNode();
                            analysePackageJson(f3, resultat2);
                            projetEnfant.setProjetNode(resultat2);
                        }
                    }
                }

            }
        }
    }

    public GroupeProjetDto getGroupeProjets() {

        Map<String, String> liste;
        String defaut = null;

        if (MapUtils.isNotEmpty(this.applicationProperties.getListeProjets())) {
            liste = this.applicationProperties.getListeProjets()
                    .entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().getNom()));
            defaut = this.applicationProperties.getListeProjets()
                    .entrySet()
                    .stream()
                    .filter(x -> x.getValue().isDefaut())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        } else {
            liste = Map.of();
        }

        GroupeProjetDto resultat = new GroupeProjetDto();
        resultat.setGroupeId(liste);
        resultat.setGroupeIdDefaut(defaut);

        return resultat;
    }

    public List<ProjetDto> getProjetDtoFromGroupId(String groupId) {
        return getProjetDto(groupId, null);
    }

//    public void updateProject4(Projet selectedProduct) throws Exception {
//        Path pomFile = Path.of(selectedProduct.getFichierPom());
//        var resultat = XmlParserService.parse(pomFile, List.of(PomParserService.PROJET_VERSION));
//        if (!CollectionUtils.isEmpty(resultat)) {
//            var res = resultat.getFirst();
//            afficheVersion(pomFile, res.positionDebut(), res.positionFin(), res.valeur());
//        }
//    }
//
//
//    public void dependancy(Projet selectedProduct, ConfigurableApplicationContext applicationContext) {
//        RunProcessUI runProcessUI = new RunProcessUI(applicationContext);
//        runProcessUI.run(List.of("cmd", "/C", "mvn", "-f", selectedProduct.getFichierPom(), "dependency:tree"));
//    }


}
