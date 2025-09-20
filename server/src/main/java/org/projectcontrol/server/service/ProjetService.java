package org.projectcontrol.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.projectcontrol.core.service.GrepService;
import org.projectcontrol.core.service.PomParserService;
import org.projectcontrol.core.utils.GrepCriteresRecherche;
import org.projectcontrol.core.utils.GrepParam;
import org.projectcontrol.core.utils.LignesRecherche;
import org.projectcontrol.server.dto.*;
import org.projectcontrol.server.enumeration.ModuleProjetEnum;
import org.projectcontrol.server.mapper.ProjetMapper;
import org.projectcontrol.server.properties.ApplicationProperties;
import org.projectcontrol.server.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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

    private Map<String, Map<String, ProjetGroupe>> listeGroupes = new HashMap<>();

    private AtomicLong idProjet = new AtomicLong(1);
    private AtomicLong idHash = new AtomicLong(1);

    @Value("${repertoireProjet:}")
    private String repertoireProjet;

//    @Autowired
//    private XmlParserService XmlParserService;
//
//    @Autowired
//    private PomParserService pomParserService;

    private final ApplicationProperties applicationProperties;

    private final ProjetMapper projetMapper;

    private final RechercheRepertoireService rechercheRepertoireService;

    private final PomParserService pomParserService;

    private final ChangementConfigService changementConfigService;

    private final GrepService grepService;

    public ProjetService(ApplicationProperties applicationProperties, ProjetMapper projetMapper,
                         RechercheRepertoireService rechercheRepertoireService,
                         PomParserService pomParserService, ChangementConfigService changementConfigService,
                         GrepService grepService) {
        this.projetMapper = projetMapper;
        this.rechercheRepertoireService = rechercheRepertoireService;
        this.pomParserService = pomParserService;
        this.changementConfigService = changementConfigService;
        LOGGER.info("creation repertoireProjet: {}", repertoireProjet);
        this.applicationProperties = applicationProperties;
        this.grepService = grepService;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("init repertoireProjet: {}", repertoireProjet);
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

        if (!listeGroupes.containsKey(groupId)) {
            listeGroupes.put(groupId, new HashMap<>());
        }

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

        var mapProjets = listeGroupes.get(groupId);

        return listePom(rep, directoriesExclude, mapProjets, groupId);
    }

//    private List<Projet> listePom(String directoryPath) {
//        return listePom(List.of(directoryPath), null);
//    }

    private List<Projet> listePom(List<String> directoryPath, Set<String> directoriesExclude,
                                  Map<String, ProjetGroupe> mapProjets, String groupId) {
        try {
            LOGGER.info("récupération des fichiers pom ...");
            List<Projet> pomFiles = new ArrayList<>();
            for (String path : directoryPath) {
                Path p = Paths.get(path).toAbsolutePath().normalize();
                List<Projet> pomFiles2 = rechercheRepertoireService.findPomFiles(p, directoriesExclude);
                for (var pom : pomFiles2) {
                    if (pomFiles.stream()
                            .noneMatch(x -> Objects.equals(x.getRepertoire(), pom.getRepertoire()))) {
                        pomFiles.add(pom);
                        var tmp = mapProjets.entrySet().stream()
                                .filter(x -> Objects.equals(x.getValue().getRepertoire(),
                                        pom.getRepertoire()))
                                .findAny();
                        if (tmp.isPresent()) {
                            pom.setId(tmp.get().getKey());
                            tmp.get().getValue().setProjet(pom);
                        } else {
                            var id = "" + idProjet.getAndIncrement();
                            pom.setId(id);
                            ProjetGroupe projetGroupe = new ProjetGroupe();
                            projetGroupe.setNomProjet(pom.getNom());
                            projetGroupe.setId(id);
                            projetGroupe.setRepertoire(pom.getRepertoire());
                            projetGroupe.setIdGroupe(groupId);
                            projetGroupe.setProjet(pom);
                            mapProjets.put(id, projetGroupe);
                        }
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

    public ListVersionDto getListeVersion(String groupId, String nomProjet) {
        LOGGER.info("LisiteVersion");
        List<Projet> liste = getProjets(groupId, nomProjet);
        if (liste != null && liste.size() == 1) {
            Projet projet = liste.getFirst();

            if (projet.getProjetPom() != null &&
                    projet.getProjetPom().getArtifact() != null) {
                var version = projet.getProjetPom().getArtifact().version();
                if (StringUtils.isNotBlank(version)) {
                    List<String> listeVersion = getListeVersion(version);
                    List<FichierAModifieDto> listeFichierAModifie = rechercheVersion(projet, version);
                    ListVersionDto listVersionDto = new ListVersionDto();
                    listVersionDto.setVersionActuelle(version);
                    listVersionDto.setListeVersions(listeVersion);
                    listVersionDto.setMessageCommit("commit version VERSION");
                    listVersionDto.setFichierAModifier(listeFichierAModifie);
                    return listVersionDto;
                }
            }
        }
        return null;
    }

    private List<FichierAModifieDto> rechercheVersion(Projet projet, String version) {
        List<FichierAModifieDto> listeFichiersAModifie = new ArrayList<>();
        String repertoire = projet.getRepertoire();
        GrepParam grepParam = new GrepParam();
        grepParam.setRepertoires(List.of(repertoire));
        grepParam.setExclusions(GrepService.REPERTOIRES_EXCLUSION);
        grepParam.setExtensionsFichiers(List.of("xml"));
        grepParam.setNbLignesAutour(3);
        GrepCriteresRecherche criteresRecherche = new GrepCriteresRecherche();
        criteresRecherche.setTexte(List.of(version));
        grepParam.setCriteresRecherche(criteresRecherche);
        try {
            List<LigneResultatDto> resultatDtoList = new ArrayList<>();
            boolean erreur[] = new boolean[]{false};
            grepService.search(grepParam)
                    .subscribe(ligne -> {
                        if (ligne != null) {
                            if (Objects.equals(ligne.ficher().getFileName().toString(), "pom.xml")) {
                                resultatDtoList.add(convertie(ligne, Path.of(repertoire)));
                            }
                        }
                    }, (error) -> {
                        LOGGER.error("Erreur lors de l'analyse du projet {}", repertoire, error);
                        erreur[0] = true;
                    }, () -> {
                        //fini = true;
                    });
            LOGGER.info("ResultatDtoList {}", resultatDtoList);
            if (!erreur[0]) {
                listeFichiersAModifie = convertieResultatRecherche(resultatDtoList);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur", e);
        }
        LOGGER.info("listeFichiersAModifie {}", listeFichiersAModifie);
        return listeFichiersAModifie;
    }

    private List<FichierAModifieDto> convertieResultatRecherche(List<LigneResultatDto> resultatDtoList) {
        List<FichierAModifieDto> listeFichiersAModifie = new ArrayList<>();

        for (LigneResultatDto ligne : resultatDtoList) {
            FichierAModifieDto fichierAModifie = new FichierAModifieDto();
            fichierAModifie.setNomFichier(ligne.getFichier());
            fichierAModifie.setHash(hash());
            if (!CollectionUtils.isEmpty(ligne.getLignes2())) {
                for (var ligne2 : ligne.getLignes2()) {
                    int noLigne2 = ligne2.getNoLigne();
                    String s = ligne2.getLigne();
                    if (ligne2.isTrouve()) {
                        fichierAModifie.getLignes().put(noLigne2, new LigneAModifierDto(noLigne2, s, true, ligne2.getRange()));
                    } else {
                        fichierAModifie.getLignes().put(noLigne2, new LigneAModifierDto(noLigne2, s, false, null));
                    }
                }
            }
            listeFichiersAModifie.add(fichierAModifie);
        }

        return listeFichiersAModifie;
    }

    private String hash() {
        var s = "%05d".formatted(idHash.getAndIncrement());
        String sha256hex = Hashing.sha256()
                .hashString(s, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
    }

    private LigneResultatDto convertie(LignesRecherche ligne, Path repertoireProjet) {
        var ligneResultatDto = new LigneResultatDto();
        ligneResultatDto.setNoLigne(ligne.noLigneDebut());
        ligneResultatDto.setLignes(ligne.lignes());
        ligneResultatDto.setLignes2(ligne.lignes2());
        Path path = ligne.ficher();
        ligneResultatDto.setFichier(repertoireProjet.relativize(path).toString());
        ligneResultatDto.setRepertoireParent(repertoireProjet.toString());
        return ligneResultatDto;
    }

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
        List<Projet> liste = getProjets(groupId, nomProjet);
        List<ProjetDto> listeResultat = new ArrayList<>();
        for (Projet projet : liste) {
            LOGGER.info("Analyse du projet {}", projet.getNom());
            try {
                var projetDto = new ProjetDto();
                analyseProjet(projet);
                projetDto.setNom(projet.getNom());
                if (projet.getProjetPom() != null) {
                    var pom = projet.getProjetPom();
                    copiePom(pom, projetDto);
                }
                var properties = projetDto.getProperties();
                if (properties != null) {
                    properties = new TreeMap<>(properties);
                }
                this.projetMapper.projetToProjetDto(projet, projetDto);
                projetDto.setProperties(properties);
                completeProjetDto(projetDto);
                listeResultat.add(projetDto);
            } catch (Exception e) {
                LOGGER.error("Erreur lors de l'analyse du projet {}", projet.getNom(), e);
            }
        }
        return listeResultat;
    }

    private List<Projet> getProjets(String groupId, String nomProjet) {
        List<Projet> liste;
        if (nomProjet != null && listeGroupes.containsKey(groupId) && listeGroupes.get(groupId).containsKey(nomProjet) &&
                listeGroupes.get(groupId).get(nomProjet).getProjet() != null) {
            liste = rechercheProjet(groupId, nomProjet);
        } else {
            liste = getProjets2(groupId);
            if (liste != null && !liste.isEmpty() && StringUtils.isNotBlank(nomProjet) &&
                    liste.stream().anyMatch(p -> p.getNom().equals(nomProjet))) {
                liste = liste.stream().filter(p -> p.getNom().equals(nomProjet)).toList();
            }
        }
        return liste;
    }

    private List<Projet> rechercheProjet(String groupId, String nomProjet) {
        List<Projet> liste = new ArrayList<>();
        if (listeGroupes.containsKey(groupId)) {
            var map = listeGroupes.get(groupId);
            if (map.containsKey(nomProjet)) {
                var groupeProjet = map.get(nomProjet);
                if (groupeProjet != null && groupeProjet.getProjet() != null) {
                    liste.add(groupeProjet.getProjet());
                }
            }
        }
        return liste;
    }

    private void completeProjetDto(ProjetDto projetDto) {
        if (projetDto.getModules() == null) {
            projetDto.setModules(new HashSet<>());
        }
        if (projetDto.getDetailModules() == null) {
            projetDto.setDetailModules(new HashMap<>());
        }
        if (projetDto.getProperties() != null) {
            if (projetDto.getProperties().containsKey("java")) {
                projetDto.getModules().add(ModuleProjetEnum.JAVA);
                projetDto.getDetailModules().put("java", projetDto.getProperties().get("java"));
            } else if (projetDto.getProperties().containsKey("maven.compiler.target")) {
                projetDto.getModules().add(ModuleProjetEnum.JAVA);
                projetDto.getDetailModules().put("java", projetDto.getProperties().get("maven.compiler.target"));
            }
        }
        if (projetDto.getArtifact() != null || projetDto.getParent() != null) {
            projetDto.getModules().add(ModuleProjetEnum.POM);
            if (projetDto.getParent() != null) {
                if (Objects.equals(projetDto.getParent().getGroupId(), "org.springframework.boot")) {
                    projetDto.getModules().add(ModuleProjetEnum.SPRING_BOOT);
                    var versionSpringBoot = projetDto.getParent().getVersion();
                    if (StringUtils.isNotBlank(versionSpringBoot)) {
                        projetDto.getDetailModules().put("spring-boot", versionSpringBoot);
                    }
                }
            }
            if (!projetDto.getModules().contains(ModuleProjetEnum.SPRING_BOOT)) {
                if (projetDto.getDependencies() != null) {
                    var springPresent = projetDto.getDependencies().stream()
                            .filter(x -> Objects.equals(x.groupId(), "org.springframework.boot"))
                            .findAny();
                    if (springPresent.isPresent()) {
                        projetDto.getModules().add(ModuleProjetEnum.SPRING_BOOT);
                        var versionSpringBoot = springPresent.get().version();
                        if (StringUtils.isNotBlank(versionSpringBoot)) {
                            projetDto.getDetailModules().put("spring-boot", versionSpringBoot);
                        }
                    }
                }
            }
        }
        if (projetDto.getInfoGit() != null) {
            projetDto.getModules().add(ModuleProjetEnum.GIT);
        }
        if (projetDto.getInfoNode() != null) {
            projetDto.getModules().add(ModuleProjetEnum.NODEJS);
            if (projetDto.getInfoNode().getDependencies() != null) {
                if (projetDto.getInfoNode().getDependencies().containsKey("@angular/core")) {
                    projetDto.getModules().add(ModuleProjetEnum.ANGULAR);
                    projetDto.getDetailModules().put("angular", projetDto.getInfoNode().getDependencies().get("@angular/core"));
                }
            }
        }
        if (projetDto.getProjetEnfants() != null) {
            for (var enfant : projetDto.getProjetEnfants()) {
                completeProjetDto(enfant);
            }
        }
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

            Git git = new Git(repository);
            RevCommit latestCommit = git.
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
            resultat.setDate(LocalDateTime.ofInstant(date, ZoneOffset.systemDefault()));

            Status status = git.status().call();
            if (!status.isClean()) {
                resultat.setFichiersNonTracke(List.of(status.getUntracked().toArray(new String[0])));
                resultat.setFichiersNonCommite(List.of(status.getModified().toArray(new String[0])));
            } else {
                resultat.setFichiersNonCommite(List.of());
                resultat.setFichiersNonTracke(List.of());
            }

            resultat.setListeBranchesCommit(new ArrayList<>());
            ObjectId head = repository.resolve("HEAD");
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(head);

                List<Ref> branches = git.branchList()
                        .setListMode(ListBranchCommand.ListMode.ALL)
                        .call();

                LOGGER.info("Branches contenant le commit courant :");
                for (Ref ref : branches) {
                    boolean contains = git.branchList()
                            .setContains(commit.getName())
                            .call()
                            .stream()
                            .anyMatch(r -> r.getName().equals(ref.getName()));

                    if (contains) {
                        LOGGER.info("  {}", ref.getName());
                        resultat.getListeBranchesCommit().add(ref.getName());
                    }
                }
            }

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
                        map.put(script.getKey(), script.getValue().asText());
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
                        map.put(script.getKey(), script.getValue().asText());
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
                        map.put(script.getKey(), script.getValue().asText());
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
                        rechercheRepertoireService.completeProjet(f, projetEnfant);
                        analysePom(f2, projetEnfant);
                        ProjetPom projetPom2 = null;
                        if (projetEnfant.getProjetPom() != null) {
                            if (projetPom.getProjetPomEnfants() == null) {
                                projetPom.setProjetPomEnfants(new ArrayList<>());
                            }
                            if (projetEnfant.getNom() != null && projetEnfant.getProjetPom().getNom() == null) {
                                projetEnfant.getProjetPom().setNom(projetEnfant.getNom());
                            }
                            projetPom2 = projetEnfant.getProjetPom();
                            projetPom.getProjetPomEnfants().add(projetPom2);
                        }

//
                        var f3 = f.resolve("package.json");
                        if (Files.exists(f3)) {
                            ProjetNode resultat2 = new ProjetNode();
                            analysePackageJson(f3, resultat2);
                            projetEnfant.setProjetNode(resultat2);
                            if (projetPom2 == null) {
                                projetPom2 = new ProjetPom();
                                projetPom2.setNom(projetEnfant.getNom());
                                projetPom2.getProjetPomEnfants().add(projetPom2);
                            }
                            projetPom2.setProjetNode(resultat2);
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

    public void updateVersion(String groupId, String nomProjet, MajVersionDto majVersion) throws Exception {
        String version = majVersion.getVersion();
        List<Projet> liste = getProjets(groupId, nomProjet);
        if (liste != null && liste.size() == 1) {
            Projet projet = liste.getFirst();

            if (StringUtils.isNotBlank(projet.getFichierPom()) &&
                    projet.getProjetPom() != null &&
                    projet.getProjetPom().getArtifact() != null) {
                var versionPom = projet.getProjetPom().getArtifact().version();
                var pomFile = projet.getFichierPom();
                LOGGER.info("mise à jour de {} pour la version {} -> {}", pomFile, versionPom, version);
                pomParserService.updateVersion(Path.of(pomFile), version,
                        majVersion.isCommit(), majVersion.getMessageCommit());
            }
        }
    }

    public ChangementConfigDto getChangementConfig(String groupId, String nomProjet, String commitInitial, String commitFinal) throws Exception {

        ChangementConfigDto resultat = new ChangementConfigDto();
        resultat.setResultat(List.of());
        if (StringUtils.isNotBlank(commitInitial) && StringUtils.isNotBlank(commitFinal)) {
            List<Projet> liste = getProjets(groupId, nomProjet);
            if (liste != null && liste.size() == 1) {
                Projet projet = liste.getFirst();
                if (projet.getRepertoire() != null) {
                    Path path = Path.of(projet.getRepertoire());
                    String s = changementConfigService.calculDifference(path, commitInitial, commitFinal);
                    LOGGER.info("changement de config : {}", s);
                    if (s != null) {
                        String[] lines = s.split("\\r?\\n");
                        if (lines != null && lines.length > 0) {
                            resultat.setResultat(Arrays.asList(lines));
                        }
                    }
                }
            }
        }
        return resultat;
    }

    public List<LigneGitDto> getGitCommentaire(String groupId, String nomProjet, int nbCommit) {
        List<LigneGitDto> resultat = new ArrayList<>();
        List<Projet> liste = getProjets(groupId, nomProjet);
        if (liste != null && liste.size() == 1) {

            Projet projet = liste.getFirst();
            try {
                File repoDir = new File(projet.getRepertoire(), ".git");
                Repository repo = new FileRepositoryBuilder()
                        .setGitDir(repoDir)
                        .readEnvironment()
                        .findGitDir()
                        .build();

                try (Git git = new Git(repo)) {

                    ByteArrayOutputStream out = new ByteArrayOutputStream();

// --- 1. Diff des modifications non commitées ---
                    ByteArrayOutputStream workingDiffOut = new ByteArrayOutputStream();
                    DiffFormatter diffFormatter = new DiffFormatter(workingDiffOut);
                    diffFormatter.setRepository(repo);
                    diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                    diffFormatter.setDetectRenames(true);

                    ObjectReader reader = repo.newObjectReader();
                    CanonicalTreeParser headTreeIter = new CanonicalTreeParser();
                    ObjectId headTree = repo.resolve("HEAD^{tree}");
                    headTreeIter.reset(reader, headTree);

                    FileTreeIterator workingTreeIter = new FileTreeIterator(repo);

                    List<DiffEntry> diffs = diffFormatter.scan(headTreeIter, workingTreeIter);
                    for (DiffEntry entry : diffs) {
                        diffFormatter.format(entry);
                    }

                    out.write("### Uncommitted changes ###\n".getBytes());
                    out.write(workingDiffOut.toByteArray());

                    if (nbCommit > 0) {
                        // --- 2. Patch des 3 derniers commits ---
                        Iterable<RevCommit> commits = git.log()
                                .setMaxCount(nbCommit)
                                .call();

                        for (RevCommit commit : commits) {
                            if (commit.getParentCount() == 0) {
                                continue; // pas de parent (ex: premier commit)
                            }

                            ByteArrayOutputStream commitPatchOut = new ByteArrayOutputStream();
                            DiffFormatter commitFormatter = new DiffFormatter(commitPatchOut);
                            commitFormatter.setRepository(repo);
                            commitFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                            commitFormatter.setDetectRenames(true);

                            RevCommit parent = commit.getParent(0);
                            commitFormatter.format(parent, commit);

                            out.write(("\n### Commit " + commit.getName() + " ###\n").getBytes());
                            out.write(commitPatchOut.toByteArray());
                        }
                    }


                    String text = out.toString(StandardCharsets.UTF_8);

                    String[] lines = text.split("\\R");

                    List<LigneGitDto> list = Arrays.stream(lines)
                            .map(x -> {
                                LigneGitDto ligne = new LigneGitDto();
                                if (StringUtils.isNotBlank(x)) {
                                    if (x.startsWith("+")) {
                                        if (StringUtils.containsAny(x, "//", "/*", "*/")) {
                                            ligne.setCommentaire(true);
                                        }
                                    }
                                }
                                ligne.setLigne(x);
                                return ligne;
                            }).toList();

                    resultat.addAll(list);
                }

                LOGGER.info("Patch généré");
            } catch (Exception e) {
                LOGGER.error("Erreur pour générer le patch", e);
                throw new RuntimeException(e);
            }
        }
        return resultat;
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
