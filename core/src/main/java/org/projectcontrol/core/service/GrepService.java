package org.projectcontrol.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;

@Service
public class GrepService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrepService.class);

    public static final String REPERTOIRES_EXCLUSION_STR = "node_modules,target,.git,vite,node,dist";

    public static final List<String> REPERTOIRES_EXCLUSION;

    public static final String EXTENSIONS_FICHIERS_DEFAULT_STR = "java,ts,xml,html,css,scss,js,json,md,htm,py," +
            "go,rs,txt,yml,yaml,sql";
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
                    searchInFile(file, processor, cacheCriteresRecherche, extension, grepParam.getNbLignesAutour());

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
                              CacheCriteresRecherche cacheCriteresRecherche, String extension, int nbLignesAutour) throws IOException {
        if (cacheCriteresRecherche.isRechercheTextuel()) {
            recherche5(file.toString(), nbLignesAutour, processor, cacheCriteresRecherche);
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

    private void recherche5(String cheminFichier, int avant,
                            Sinks.Many<LignesRecherche> processor,
                            CacheCriteresRecherche cacheCriteresRecherche) {

        Flux<FileFlux.Ligne> flux = FileFlux.lire(cheminFichier);

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
