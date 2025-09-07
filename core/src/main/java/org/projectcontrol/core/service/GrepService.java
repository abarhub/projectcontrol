package org.projectcontrol.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectcontrol.core.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;

@Service
public class GrepService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrepService.class);

    public static final String REPERTOIRES_EXCLUSION_STR = "node_modules,target,.git,vite";

    public static final List<String> REPERTOIRES_EXCLUSION;

    public static final String EXTENSIONS_FICHIERS_DEFAULT_STR = "java,ts,xml,html,css,scss,js,json,md,htm,py," +
            "go,rs,txt,yml,yaml";
    public static final List<String> EXTENSIONS_FICHIERS_DEFAULT;

    private static final List<String> EXTENSION_JSON = List.of("json");
    private static final List<String> EXTENSION_YAML = List.of("yml", "yaml");
    private static final List<String> EXTENSION_XML = List.of("xml");

    private static final Splitter SPLITTER = Splitter.on(',')
            .omitEmptyStrings()
            .trimResults();

    static {
        REPERTOIRES_EXCLUSION = SPLITTER.splitToList(REPERTOIRES_EXCLUSION_STR);
        EXTENSIONS_FICHIERS_DEFAULT = SPLITTER.splitToList(EXTENSIONS_FICHIERS_DEFAULT_STR);
    }

    public Flux<LignesRecherche> search(GrepParam grepParam) throws IOException {
        if (!verifieCritereRecherche(grepParam.getCriteresRecherche())) {
            LOGGER.debug("pas de texte à chercher");
            //return Observable.empty();
            return Flux.empty();
        }

        Sinks.Many<LignesRecherche> sink = Sinks.many().multicast().onBackpressureBuffer();

        Flux<LignesRecherche> hotFlux = sink.asFlux();


        try {
            List<String> repertoires = grepParam.getRepertoires();
            for (String repertoire : repertoires) {
                if (StringUtils.isNotBlank(repertoire)) {
                    search(repertoire, sink, grepParam);
                }
            }
        } catch (Exception e) {
            sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST);
        } finally {
            sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        }

        return hotFlux;
    }

    private boolean verifieCritereRecherche(GrepCriteresRecherche criteresRecherche) {
        if (criteresRecherche == null) {
            return false;
        } else {
            // toutes les listes sont vides
            return !ListUtils.isEmpty(criteresRecherche.getTexte()) ||
                    !ListUtils.isEmpty(criteresRecherche.getRegex()) ||
                    !ListUtils.isEmpty(criteresRecherche.getChamps()) ||
                    !ListUtils.isEmpty(criteresRecherche.getXpath());
        }
    }


    private void search(String repertoire, Sinks.Many<LignesRecherche> processor,
                        GrepParam grepParam) throws IOException {
        Path startDir = Paths.get(repertoire);

        Set<String> repertoireExclu = new HashSet<>();
        if (CollectionUtils.isNotEmpty(grepParam.getExclusions())) {
            repertoireExclu.addAll(grepParam.getExclusions());
        }

        CacheCriteresRecherche cacheCriteresRecherche = new CacheCriteresRecherche(grepParam);

        Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                if (Files.isDirectory(dir) && repertoireExclu.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE; // on ignore
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!Files.isRegularFile(file)) {
                    return FileVisitResult.CONTINUE;
                }

                String filename = file.getFileName().toString().toLowerCase();
                String extension = FilenameUtils.getExtension(filename);

                if (!bonneExtention(grepParam, extension)) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    searchInFile(file, processor, cacheCriteresRecherche, extension);

                } catch (Exception e) {
                    LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean bonneExtention(GrepParam grepParam, String extension) {
        if (CollectionUtils.isNotEmpty(grepParam.getExtensionsFichiers())) {

            if (StringUtils.isNotBlank(extension)) {
                return grepParam.getExtensionsFichiers().contains(extension);
            }
            return false;
        } else {
            return true;
        }
    }

    private void searchInFile(Path file, Sinks.Many<LignesRecherche> processor,
                              CacheCriteresRecherche cacheCriteresRecherche, String extension) throws IOException {
        if (cacheCriteresRecherche.isRechercheTextuel()) {
            //litTexte(file, cacheCriteresRecherche,processor);
            recherche5(file.toString(), "classe", 3, processor, cacheCriteresRecherche);
        }
        if (cacheCriteresRecherche.isRechercheChamps()) {
            if (CollectionUtils.containsAny(EXTENSION_JSON, extension)) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.enable(ALLOW_COMMENTS);
                    try (InputStream inputStream = Files.newInputStream(file)) {
                        JsonNode jsonNodeInitial = objectMapper.readTree(inputStream);
                        if (jsonNodeInitial != null) {
                            for (var chemin : cacheCriteresRecherche.getListeChemins()) {
                                JsonNode jsonNode = jsonNodeInitial;
                                if (jsonNode.has(chemin.getFirst())) {
                                    for (int i = 0; i < chemin.size(); i++) {
                                        if (jsonNode.has(chemin.get(i))) {
                                            jsonNode = jsonNode.get(chemin.get(i));
                                        } else {
                                            jsonNode = null;
                                            break;
                                        }
                                    }
                                    if (jsonNode != null) {
                                        String texte = Joiner.on('.').join(chemin) + "=" + jsonNode;
                                        LignesRecherche l = new LignesRecherche(0, List.of(texte), null, file, List.of(0));
                                        LOGGER.debug("ajout de {}", l);
                                        processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
                }
            }
            if (CollectionUtils.containsAny(EXTENSION_YAML, extension)) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = Files.newInputStream(file)) {
                    Map<Object, Object> documentInitial = yaml.load(inputStream);
                    if (documentInitial != null) {
                        for (var chemin : cacheCriteresRecherche.getListeChemins()) {
                            if (documentInitial.containsKey(chemin.getFirst())) {
                                var documentStr = parcourtYml(documentInitial, chemin);
                                if (documentStr != null) {
                                    String texte = Joiner.on('.').join(chemin) + ": " + documentStr;
                                    LignesRecherche l = new LignesRecherche(0, List.of(texte), null, file, List.of(0));
                                    LOGGER.debug("ajout de {}", l);
                                    processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
                }

            }
        }

        if (cacheCriteresRecherche.isRechercheXPath()) {
            if (CollectionUtils.containsAny(EXTENSION_XML, extension)) {

                try (InputStream xmlFile = Files.newInputStream(file)) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document xml = db.parse(xmlFile);
                    xml.getDocumentElement().normalize();

                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xpath = xpf.newXPath();

                    for (var chemin : cacheCriteresRecherche.getCheminXPath()) {

                        NodeList nodeList = (NodeList) xpath.evaluate(chemin, xml, XPathConstants.NODESET);
                        if (nodeList != null && nodeList.getLength() > 0) {
                            String texte = "";
                            if (nodeList.getLength() == 1) {
                                texte = nodeList.item(0).getTextContent();
                            } else {
                                StringBuilder texte2 = new StringBuilder();
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    if (i > 0) {
                                        texte2.append(";");
                                    }
                                    texte2.append(nodeList.item(i).getTextContent());
                                }
                                texte = texte2.toString();
                            }
                            String name = chemin + ": " + texte;
                            LignesRecherche l = new LignesRecherche(0, List.of(name), null, file, List.of(0));
                            LOGGER.debug("ajout de {}", l);
                            processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                        }
                    }


                } catch (Exception e) {
                    LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
                }
            }
        }
    }

    private void litTexte(Path file, CacheCriteresRecherche cacheCriteresRecherche, Sinks.Many<LignesRecherche> processor) {
        int nbLigne = 3;
        CircularFifoQueue<LigneRecherche> queue = new CircularFifoQueue<>(nbLigne);
        try (Stream<String> stream = Files.lines(file)) {
            int[] tab = new int[1];
            Map<Integer, String> map = new TreeMap<>();
            Set<Integer> lignesTrouve = new TreeSet<>();
            NavigableMap<Integer, AnalyseLigne> lignesAnalysees = new TreeMap<>();
            stream
                    .peek(line -> {
                        tab[0]++;
                        queue.add(new LigneRecherche(tab[0], line));
                    })
                    .filter(cacheCriteresRecherche::contientTexte)
                    .forEach(x -> {
                        int noLigne = tab[0];

                        AnalyseLigne analyseLigne = new AnalyseLigne();
                        analyseLigne.setLigne(x);
                        analyseLigne.setNoLigne(noLigne);
                        analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_NON_ANALYSE);
                        if (cacheCriteresRecherche.contientTexte(x)) {
                            lignesTrouve.add(noLigne);
                            analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_TROUVEE);
                        }
                        map.put(noLigne, x);
                        lignesAnalysees.put(noLigne, analyseLigne);
                        nettoyage(map, noLigne, lignesTrouve, nbLigne, lignesAnalysees);


                        List<String> listeLigne = new ArrayList<>();
                        listeLigne.add(x);
                        List<Integer> listeNoLigne = new ArrayList<>();
                        listeNoLigne.add(noLigne);
                        LignesRecherche l = new LignesRecherche(noLigne, listeLigne, null, file, listeNoLigne);
                        LOGGER.debug("ajout de {}", l);
                        processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                    });
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
        }
    }

    private void litTexte2(Path file, CacheCriteresRecherche cacheCriteresRecherche, Sinks.Many<LignesRecherche> processor) {
        int nbLigne = 3;
        CircularFifoQueue<LigneRecherche> queue = new CircularFifoQueue<>(nbLigne);
        try (Stream<String> stream = Files.lines(file)) {
            int[] tab = new int[1];
            Map<Integer, String> map = new TreeMap<>();
            Set<Integer> lignesTrouve = new TreeSet<>();
            NavigableMap<Integer, AnalyseLigne> lignesAnalysees = new TreeMap<>();
            stream
                    .peek(line -> {
                        tab[0]++;
                        queue.add(new LigneRecherche(tab[0], line));
                    })
                    .filter(cacheCriteresRecherche::contientTexte)
                    .forEach(x -> {
                        int noLigne = tab[0];

                        AnalyseLigne analyseLigne = new AnalyseLigne();
                        analyseLigne.setLigne(x);
                        analyseLigne.setNoLigne(noLigne);
                        analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_NON_ANALYSE);
                        if (cacheCriteresRecherche.contientTexte(x)) {
                            lignesTrouve.add(noLigne);
                            analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_TROUVEE);
                        }
                        map.put(noLigne, x);
                        lignesAnalysees.put(noLigne, analyseLigne);
                        nettoyage(map, noLigne, lignesTrouve, nbLigne, lignesAnalysees);


                        List<String> listeLigne = new ArrayList<>();
                        listeLigne.add(x);
                        List<Integer> listeNoLigne = new ArrayList<>();
                        listeNoLigne.add(noLigne);
                        LignesRecherche l = new LignesRecherche(noLigne, listeLigne, null, file, listeNoLigne);
                        LOGGER.debug("ajout de {}", l);
                        processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                    });
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
        }
    }

    private void nettoyage(Map<Integer, String> map, int noLigne, Set<Integer> lignesTrouve,
                           int nbLigne, Map<Integer, AnalyseLigne> lignesAnalysees) {
        var ligne = lignesAnalysees.get(noLigne);
        if (ligne.getEtatLigne() == EtatLigneEnum.LIGNE_TROUVEE) {
            return;
        } else if (ligne.getEtatLigne() == EtatLigneEnum.LIGNE_AUTOUR) {
            return;
        } else {

            for (int i = noLigne; i >= 0; i--) {
                var ligne2 = lignesAnalysees.get(i);
                if (ligne2.getEtatLigne() == EtatLigneEnum.LIGNE_TROUVEE) {
                    for (int j = 0; j < nbLigne && i + j <= noLigne; j++) {
                        var ligne3 = lignesAnalysees.get(i + j);
                        if (ligne3.getEtatLigne() == EtatLigneEnum.LIGNE_NON_ANALYSE) {
                            ligne3.setEtatLigne(EtatLigneEnum.LIGNE_AUTOUR);
                        }
                    }
                    break;
                }
            }
        }
        if (lignesTrouve.contains(noLigne)) {

        } else {
            var iter = map.entrySet().iterator();
            int max = lignesTrouve.stream().max(Comparator.naturalOrder()).orElse(0);
            while (iter.hasNext()) {
                var entry = iter.next();
                var key = entry.getKey();
                if (lignesTrouve.contains(key)) {
                    break;
                } else {

                }

                if (Math.abs(key - noLigne) < nbLigne) {

                }
                if (entry.getValue().contains(String.valueOf(noLigne - nbLigne))) {
                    iter.remove();
                }
            }
            for (var entry : map.entrySet()) {
                if (entry.getValue().contains(String.valueOf(noLigne))) {
                    lignesTrouve.add(entry.getKey());
                    break;
                }
            }
        }
    }

    public static Flux<String> rechercher(String cheminFichier, String motif, int avant, int apres) {
        return Flux.using(
                () -> new BufferedReader(new FileReader(cheminFichier)),
                reader -> {
                    Deque<String> bufferAvant = new ArrayDeque<>(avant);
                    int[] compteurApres = {0};

                    return Flux.fromStream(reader.lines())
                            .index()
                            .handle((tuple, sink) -> {
                                long numLigne = tuple.getT1() + 1; // index commence à 0
                                String ligne = tuple.getT2();

                                if (ligne.contains(motif)) {
                                    // on vide le buffer "avant"
                                    long start = numLigne - bufferAvant.size();
                                    for (String l : bufferAvant) {
                                        sink.next((start++) + ": " + l);
                                    }
                                    // ligne courante
                                    sink.next(numLigne + ": " + ligne);
                                    compteurApres[0] = apres;
                                } else if (compteurApres[0] > 0) {
                                    sink.next(numLigne + ": " + ligne);
                                    compteurApres[0]--;
                                }

                                // ajouter la ligne courante dans le buffer
                                if (avant > 0) {
                                    if (bufferAvant.size() == avant) {
                                        bufferAvant.pollFirst();
                                    }
                                    bufferAvant.addLast(ligne);
                                }
                            });
                },
                reader -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }


    public static Flux<String> rechercher2(String cheminFichier, String motif, int avant, int apres) {
        return Flux.using(
                () -> new BufferedReader(new FileReader(cheminFichier)),
                reader -> {
                    Deque<String> bufferAvant = new ArrayDeque<>(avant);
                    int[] compteurApres = {0};

                    return Flux.fromStream(reader.lines())
                            .index()
                            .handle((tuple, sink) -> {
                                long numLigne = tuple.getT1() + 1; // index commence à 0
                                String ligne = tuple.getT2();

                                if (ligne.contains(motif)) {
                                    // on vide le buffer "avant"
                                    long start = numLigne - bufferAvant.size();
                                    for (String l : bufferAvant) {
                                        sink.next((start++) + ": " + l);
                                    }
                                    // ligne courante
                                    sink.next(numLigne + ": " + ligne);
                                    compteurApres[0] = apres;
                                } else if (compteurApres[0] > 0) {
                                    sink.next(numLigne + ": " + ligne);
                                    compteurApres[0]--;
                                }

                                // ajouter la ligne courante dans le buffer
                                if (avant > 0) {
                                    if (bufferAvant.size() == avant) {
                                        bufferAvant.pollFirst();
                                    }
                                    bufferAvant.addLast(ligne);
                                }
                            });
                },
                reader -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    public static Flux<String> rechercher3(String cheminFichier, String motif, int avant, int apres) {
        return Flux.generate(
                () -> {
                    try {
                        return new State(new BufferedReader(new FileReader(cheminFichier)), avant, apres, motif);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (state, sink) -> {
                    try {
                        String ligne = state.reader.readLine();
                        if (ligne == null) {
                            sink.complete();
                            return state;
                        }

                        state.numLigne.incrementAndGet();

                        if (ligne.contains(state.motif)) {
                            // émettre les lignes avant
                            long start = state.numLigne.get() - state.bufferAvant.size();
                            for (String l : state.bufferAvant) {
                                sink.next(start++ + ": " + l);
                            }
                            // ligne courante
                            sink.next(state.numLigne.get() + ": " + ligne);
                            state.compteurApres = state.maxApres;
                        } else if (state.compteurApres > 0) {
                            sink.next(state.numLigne.get() + ": " + ligne);
                            state.compteurApres--;
                        }

                        // mettre à jour le buffer "avant"
                        if (state.maxAvant > 0) {
                            if (state.bufferAvant.size() == state.maxAvant) {
                                state.bufferAvant.pollFirst();
                            }
                            state.bufferAvant.addLast(ligne);
                        }

                    } catch (IOException e) {
                        sink.error(e);
                    }
                    return state;
                },
                state -> {
                    try {
                        state.reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    public static Flux<AnalyseLigne> rechercher4(String cheminFichier, String motif, int avant, int apres) {
        return Flux.generate(
                () -> {
                    try {
                        return new State(new BufferedReader(new FileReader(cheminFichier)), avant, apres, motif);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (state, sink) -> {
                    try {
                        String ligne = state.reader.readLine();
                        if (ligne == null) {
                            sink.complete();
                            return state;
                        }

                        state.numLigne.incrementAndGet();

                        if (ligne.contains(state.motif)) {
                            // émettre les lignes avant
                            long start = state.numLigne.get() - state.bufferAvant.size();
                            for (String l : state.bufferAvant) {
                                AnalyseLigne analyseLigne = new AnalyseLigne();
                                analyseLigne.setNoLigne((int) start++);
                                analyseLigne.setLigne(l);
                                analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_AUTOUR);
                                sink.next(analyseLigne);
//                                sink.next(start++ + ": " + l);
                            }
                            // ligne courante
                            AnalyseLigne analyseLigne = new AnalyseLigne();
                            analyseLigne.setNoLigne(state.numLigne.get());
                            analyseLigne.setLigne(ligne);
                            analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_TROUVEE);
                            sink.next(analyseLigne);
//                            sink.next(state.numLigne.get() + ": " + ligne);
                            state.compteurApres = state.maxApres;
                        } else if (state.compteurApres > 0) {
                            AnalyseLigne analyseLigne = new AnalyseLigne();
                            analyseLigne.setNoLigne(state.numLigne.get());
                            analyseLigne.setLigne(ligne);
                            analyseLigne.setEtatLigne(EtatLigneEnum.LIGNE_AUTOUR);
                            sink.next(analyseLigne);
//                            sink.next(state.numLigne.get() + ": " + ligne);
                            state.compteurApres--;
                        }

                        // mettre à jour le buffer "avant"
                        if (state.maxAvant > 0) {
                            if (state.bufferAvant.size() == state.maxAvant) {
                                state.bufferAvant.pollFirst();
                            }
                            state.bufferAvant.addLast(ligne);
                        }

                    } catch (IOException e) {
                        sink.error(e);
                    }
                    return state;
                },
                state -> {
                    try {
                        state.reader.close();
                    } catch (IOException e) {
                        LOGGER.error("Erreur", e);
                    }
                }
        );
    }

    private static void recherche5(String cheminFichier, String motif, int avant,
                                   Sinks.Many<LignesRecherche> processor,
                                   CacheCriteresRecherche cacheCriteresRecherche) {
        //rechercher4(cheminFichier, motif, avant, avant)

        Flux<FileFlux.Ligne> flux = FileFlux.lire(cheminFichier);

        if (false) {
            FileFlux.rechercher(flux, motif, avant, avant, cacheCriteresRecherche)
                    .doOnNext(x -> {
                        LOGGER.info("ligne: {}", x);
                        LignesRecherche l = new LignesRecherche(0, x.getLignes(), null,
                                Path.of(cheminFichier), List.of(0));
                        LOGGER.debug("ajout de {}", l);
                        processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                    })
                    .blockLast();
        } else {
            FileFlux2.rechercherEtFusionner(flux, List.of(), avant, avant, cacheCriteresRecherche)
                    .doOnNext(x -> {
                        LOGGER.info("ligne: {}", x);
                        LignesRecherche l = new LignesRecherche(0, null, x.getLignes(),
                                Path.of(cheminFichier), List.of(0));
                        LOGGER.debug("ajout de {}", l);
                        processor.emitNext(l, Sinks.EmitFailureHandler.FAIL_FAST);
                    })
                    .blockLast();
        }

    }

    private static class State {
        final BufferedReader reader;
        final int maxAvant;
        final int maxApres;
        final String motif;
        final Deque<String> bufferAvant;
        final AtomicInteger numLigne = new AtomicInteger(0);
        int compteurApres = 0;

        State(BufferedReader reader, int maxAvant, int maxApres, String motif) {
            this.reader = reader;
            this.maxAvant = maxAvant;
            this.maxApres = maxApres;
            this.motif = motif;
            this.bufferAvant = new ArrayDeque<>(maxAvant);
        }
    }

    private String parcourtYml(Map<Object, Object> document, List<String> chemin) {
        String texte = null;
        for (int i = 0; i < chemin.size(); i++) {
            if (document != null && document.containsKey(chemin.get(i))) {
                Object o = document.get(chemin.get(i));
                if (o == null) {
                    if (i == chemin.size() - 1) {
                        return "";
                    } else {
                        return null;
                    }
                } else if (o instanceof Map m) {
                    document = m;
                } else {
                    if (i == chemin.size() - 1) {
                        return "" + o;
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
        if (document != null) {
            return document.toString();
        }
        return texte;
    }

}
