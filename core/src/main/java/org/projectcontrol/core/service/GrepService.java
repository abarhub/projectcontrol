package org.projectcontrol.core.service;

import com.google.common.base.Splitter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.projectcontrol.core.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class GrepService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrepService.class);

    public static final String REPERTOIRES_EXCLUSION_STR = "node_modules,target,.git,vite";

    public static final List<String> REPERTOIRES_EXCLUSION;

    public static final String EXTENSIONS_FICHIERS_DEFAULT_STR = "java,ts,xml,html,css,scss,js,json,md,htm,py,go,rs,txt";
    public static final List<String> EXTENSIONS_FICHIERS_DEFAULT;

    private static final Splitter SPLITTER = Splitter.on(',')
            .omitEmptyStrings()
            .trimResults();

    static {
        REPERTOIRES_EXCLUSION = SPLITTER.splitToList(REPERTOIRES_EXCLUSION_STR);
        EXTENSIONS_FICHIERS_DEFAULT = SPLITTER.splitToList(EXTENSIONS_FICHIERS_DEFAULT_STR);
    }

    public Observable<LignesRecherche> search(GrepParam grepParam) throws IOException {
        if (!verifieCritereRecherche(grepParam.getCriteresRecherche())) {
            LOGGER.debug("pas de texte à chercher");
            return Observable.empty();
        }

        return Observable.create(emitter -> {

            try {
                List<String> repertoires = grepParam.getRepertoires();
                for (String repertoire : repertoires) {
                    if (StringUtils.isNotBlank(repertoire)) {
                        search(repertoire, emitter, grepParam);
                    }
                }
            } catch (Exception e) {
                emitter.onError(e);
            } finally {
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            }

        });
    }

    private boolean verifieCritereRecherche(GrepCriteresRecherche criteresRecherche) {
        if (criteresRecherche == null) {
            return false;
        } else {

            if (ListUtils.isEmpty(criteresRecherche.getTexte()) &&
                    ListUtils.isEmpty(criteresRecherche.getRegex()) &&
                    ListUtils.isEmpty(criteresRecherche.getChamps()) &&
                    ListUtils.isEmpty(criteresRecherche.getXpath())) {
                // toutes les listes sont vides
                return false;
            }
        }
        return true;
    }


    private void search(String repertoire, ObservableEmitter<LignesRecherche> processor,
                        GrepParam grepParam) throws IOException {
        Path startDir = Paths.get(repertoire);

        Set<String> repertoireExclu = new HashSet<>();
        if (CollectionUtils.isNotEmpty(grepParam.getExclusions())) {
            repertoireExclu.addAll(grepParam.getExclusions());
        }

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

                if (!bonneExtention(file, grepParam)) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    searchInFile(grepParam, file, processor);

                } catch (Exception e) {
                    LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean bonneExtention(Path file, GrepParam grepParam) {
        if (CollectionUtils.isNotEmpty(grepParam.getExtensionsFichiers())) {

            String filename = file.getFileName().toString().toLowerCase();
            String extension = FilenameUtils.getExtension(filename);
            if (StringUtils.isNotBlank(extension)) {
                if (grepParam.getExtensionsFichiers().contains(extension)) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private List<LignesRecherche> searchInFile(GrepParam grepParam, Path file, ObservableEmitter<LignesRecherche> processor) throws IOException {
        List<LignesRecherche> liste = new ArrayList<>();
        List<String> textes = null;
        List<String> regexes = null;
        if (CollectionUtils.isNotEmpty(grepParam.getCriteresRecherche().getTexte())) {
            textes = grepParam.getCriteresRecherche().getTexte();
        }
        if (CollectionUtils.isNotEmpty(grepParam.getCriteresRecherche().getRegex())) {
            regexes = grepParam.getCriteresRecherche().getRegex();
        }
        if (textes != null || regexes != null) {
            CircularFifoQueue<LigneRecherche> queue = new CircularFifoQueue<>(6);
            try (Stream<String> stream = Files.lines(file)) {
                int[] tab = new int[1];
                String[] texte2 = textes == null ? null : textes.toArray(new String[0]);
                List<Pattern> regexes2 = regexes == null ? null : regexes.stream().map(Pattern::compile).toList();
                stream
                        .peek(line -> {
                            tab[0]++;
                            queue.add(new LigneRecherche(tab[0], line));
                        })
                        .filter(line -> contient(line, texte2, regexes2))
                        .forEach(x -> {
                            int noLigne = tab[0];
                            List<String> listeLigne = new ArrayList<>();
                            listeLigne.add(x);
                            List<Integer> listeNoLigne = new ArrayList<>();
                            listeNoLigne.add(noLigne);
                            LignesRecherche l = new LignesRecherche(noLigne, listeLigne, file, listeNoLigne);
                            LOGGER.debug("ajout de {}", l);
                            if (!processor.isDisposed()) {
                                processor.onNext(l);
                            }
                        });
            } catch (Exception e) {
                LOGGER.error("Erreur lors de la recherche dans le fichier {}", file, e);
            }
        }

        return liste;
    }

    private boolean contient(String ligne, String[] texte, List<Pattern> regexes) {
        if (StringUtils.containsAny(ligne, texte)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(regexes)) {
            for (Pattern regex : regexes) {
                if (Pattern.matches(regex.pattern(), ligne)) {
                    return true;
                }
            }
        }
        return false;
    }
}
