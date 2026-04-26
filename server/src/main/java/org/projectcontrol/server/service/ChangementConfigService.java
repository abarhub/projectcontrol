package org.projectcontrol.server.service;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ChangementConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangementConfigService.class);

    public String compareYamlFiles(String repoPath, String commit1Hash, String commit2Hash, String yamlFilePath) throws Exception {

        StringBuilder res = new StringBuilder();

        // Construire le repository Git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build()) {

            // Récupérer les contenus des fichiers pour chaque commit
            var yamlContent1 = getFileContentFromCommit(repository, commit1Hash, yamlFilePath);
            var yamlContent2 = getFileContentFromCommit(repository, commit2Hash, yamlFilePath);

            // Parser les YAML
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setTagInspector(tag -> true);
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Map<String, String> flattenedMap3 = convertYmlFile(yamlContent1, yaml);
            Map<String, String> flattenedMap4 = convertYmlFile(yamlContent2, yaml);

            compareMap(res, flattenedMap4, flattenedMap3);
        }

        return res.toString();
    }

    @NonNull
    private Map<String, String> convertYmlFile(Optional<byte[]> yamlContent2, Yaml yaml) {
        Map<String, String> flattenedMap4;
        Map<Object, Object> parsedYaml2;
        if (yamlContent2.isEmpty()) {
            flattenedMap4 = Map.of();
        } else {
            parsedYaml2 = yaml.load(new ByteArrayInputStream(yamlContent2.get()));
            Map<Object, Object> flattenedMap2 = new TreeMap<>();
            flattenMap(parsedYaml2, "", flattenedMap2);
            flattenedMap4 = convertMap(flattenedMap2);
        }
        return flattenedMap4;
    }

    private Map<String, String> convertMap(Map<Object, Object> map) {
        Map<String, String> resultat = new TreeMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            String value = (entry.getValue() != null) ? entry.getValue().toString() : null;
            resultat.put(key, value);
        }
        return resultat;
    }

    private Optional<byte[]> getFileContentFromCommit(Repository repository, String commitHash, String filePath) throws Exception {
        ObjectId commitId = repository.resolve(commitHash);
        RevCommit commit = new org.eclipse.jgit.revwalk.RevWalk(repository).parseCommit(commitId);

        RevTree tree = commit.getTree();

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            if (!treeWalk.next()) {
                LOGGER.warn("Did not find expected file '{}' pour the hash {}", filePath, commitHash);
                return Optional.empty();
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // and then one can the loader to read the file
            loader.copyTo(outputStream);

            return Optional.of(outputStream.toByteArray());
        }

    }

    private void flattenMap(Map<Object, Object> source, String prefix, Map<Object, Object> destination) {
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? "" + entry.getKey() : prefix + "." + entry.getKey();

            if (entry.getValue() instanceof Map) {
                flattenMap((Map<Object, Object>) entry.getValue(), key, destination);
            } else {
                destination.put(key, entry.getValue());
            }
        }
    }

    private void compareMap(StringBuilder res, Map<String, String> map2, Map<String, String> map1) {
        Set<String> keysAjout = new TreeSet<>(map2.keySet());
        keysAjout.removeAll(map1.keySet());

        Set<String> keysSupprime = new TreeSet<>(map1.keySet());
        keysSupprime.removeAll(map2.keySet());

        Set<String> keysModifie = new TreeSet<>();

        for (String key : map2.keySet()) {
            if (map1.containsKey(key)) {
                if (!Objects.equals(map1.get(key), map2.get(key))) {
                    keysModifie.add(key);
                }
            }
        }

        res.append("* Parametre à ajouter : \n");
        for (String key : keysAjout) {
            res.append(key).append(": ").append(str(map2.get(key))).append("\n");
        }
        res.append("* Parametre à modifier : \n");
        for (String key : keysModifie) {
            res.append(key).append(": ").append(str(map2.get(key))).append("\n");
        }
        res.append("* Parametre à supprimer : \n");
        for (String key : keysSupprime) {
            res.append(key).append("\n");
        }
    }

    private String str(String str) {
        return Objects.requireNonNullElse(str, "");
    }

    private void compareFiles2(String file1, String file2) throws IOException {
        List<String> lines1 = Files.readAllLines(Paths.get(file1));
        List<String> lines2 = Files.readAllLines(Paths.get(file2));

        System.out.println("Différences entre les fichiers :");

        // Implémentation simple de la comparaison
        Set<String> linesSet1 = new HashSet<>(lines1);
        Set<String> linesSet2 = new HashSet<>(lines2);

        // Lignes dans le premier fichier mais pas dans le second
        Set<String> uniqueToFile1 = new HashSet<>(linesSet1);
        uniqueToFile1.removeAll(linesSet2);
        if (!uniqueToFile1.isEmpty()) {
            System.out.println("Lignes uniquement dans le premier fichier:");
            uniqueToFile1.forEach(line -> System.out.println("- " + line));
        }

        // Lignes dans le second fichier mais pas dans le premier
        Set<String> uniqueToFile2 = new HashSet<>(linesSet2);
        uniqueToFile2.removeAll(linesSet1);
        if (!uniqueToFile2.isEmpty()) {
            System.out.println("Lignes uniquement dans le second fichier:");
            uniqueToFile2.forEach(line -> System.out.println("- " + line));
        }
    }


    public String calculDifference(Path file, String commitDebut, String commitFin) throws Exception {
        LOGGER.debug("file={}", file);
        LOGGER.debug("root={}", file);

        List<String> liste = findConfigFiles(file.toString(), commitDebut, commitFin);

        StringBuilder sb = new StringBuilder();
        for (String p : liste) {
            LOGGER.debug("analyse de : {}", p);
            var s = p;
            s = normalisePath(s);
            sb.append("*** Analyse de : ").append(s).append(" ***\n");
            if (s.endsWith(".yml")) {
                sb.append(compareYamlFiles(file.toString(), commitDebut, commitFin, s));
            } else if (s.endsWith(".properties")) {
                sb.append(comparePropertiesFiles(file.toString(), commitDebut, commitFin, s));
            } else if (s.endsWith(".xml")) {
                sb.append(compareTextFiles(file.toString(), commitDebut, commitFin, s));
            } else {
                sb.append(compareTextFiles(file.toString(), commitDebut, commitFin, s));
            }
        }

        return sb.toString();

    }

    private String compareTextFiles(String repoPath, String commit1Hash, String commit2Hash, String texteFile) throws Exception {
        StringBuilder res = new StringBuilder();

        // Construire le repository Git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build()) {

            // Récupérer les contenus des fichiers pour chaque commit
            var texteContent1 = getFileContentFromCommit(repository, commit1Hash, texteFile);
            var texteContent2 = getFileContentFromCommit(repository, commit2Hash, texteFile);

            var buf1 = texteContent1.orElseGet(() -> new byte[0]);
            var buf2 = texteContent2.orElseGet(() -> new byte[0]);

            if (isBinaryFile(buf1) || isBinaryFile(buf2) || isBinaryFromFilename(texteFile)) {

                if (texteContent1.isPresent() && texteContent2.isPresent()) {
                    if (Arrays.equals(buf1, buf2)) {
                        res.append("fichier binaire identique\n");
                    } else {
                        res.append("fichier binaire modifié\n");
                    }
                } else if (texteContent1.isPresent()) {
                    res.append("fichier binaire supprimé\n");
                } else if (texteContent2.isPresent()) {
                    res.append("fichier binaire ajouté\n");
                }

            } else {

                RawText a = new RawText(buf1);
                RawText b = new RawText(buf2);

                EditList edits = new HistogramDiff().diff(
                        RawTextComparator.DEFAULT,
                        a,
                        b
                );

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                try (DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.format(edits, a, b);
                }

                res.append(out.toString(StandardCharsets.UTF_8));
            }

        }
        return res.toString();
    }

    private boolean isBinaryFromFilename(String texteFile) {
        if (texteFile == null || texteFile.isEmpty())
            return false;
        texteFile = texteFile.toLowerCase();
        return texteFile.endsWith(".bin") || texteFile.endsWith(".jpg") || texteFile.endsWith(".png") || texteFile.endsWith(".pdf")
                || texteFile.endsWith(".jks") || texteFile.endsWith(".p12");
    }

    private String comparePropertiesFiles(String repoPath, String commit1Hash, String commit2Hash, String propertiesFilePath) throws Exception {
        StringBuilder res = new StringBuilder();

        // Construire le repository Git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build()) {

            // Récupérer les contenus des fichiers pour chaque commit
            var propertiesContent1 = getFileContentFromCommit(repository, commit1Hash, propertiesFilePath);
            var propertiesContent2 = getFileContentFromCommit(repository, commit2Hash, propertiesFilePath);

            // Parser les properties
            Properties properties1 = new Properties();
            Properties properties2 = new Properties();

            String s1 = "";
            String s2 = "";
            if (propertiesContent1.isPresent()) {
                s1 = new String(propertiesContent1.get(), StandardCharsets.UTF_8);
            }
            if (propertiesContent2.isPresent()) {
                s2 = new String(propertiesContent2.get(), StandardCharsets.UTF_8);
            }
            properties1.load(new StringReader(s1));
            properties2.load(new StringReader(s2));

            Map<String, String> map1 = convertToMap(properties1);
            Map<String, String> map2 = convertToMap(properties2);

            compareMap(res, map2, map1);
        }

        return res.toString();
    }

    private Map<String, String> convertToMap(Properties properties) {
        Map<String, String> map = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }

    public List<String> findConfigFiles(String directoryPath, String oldCommitId, String newCommitId) throws Exception {
        var root = Paths.get(directoryPath);
        var liste = getListPath(root.toFile(), oldCommitId, newCommitId);

        var liste2 = findConfigFiles0(directoryPath);

        for (var path : liste2) {
            var f = root.relativize(path);
            var s = normalisePath(f.toString());
            if (!liste.contains(s)) {
                liste.add(s);
            }
        }

        liste = liste.stream()
                .sorted()
                .distinct()
                .toList();

        return liste;
    }

    public List<Path> findConfigFiles0(String directoryPath) throws IOException {
        Path startPath = Paths.get(directoryPath);

        try (Stream<Path> walk = Files.walk(startPath)) {
            return walk
                    .filter(Files::isRegularFile) // Ne traiter que les fichiers réguliers
                    .filter(path ->
                            (isConfig(path) ||
                                    (StringUtils.endsWith(path.getFileName().toString(), ".xml") ||
                                            StringUtils.endsWith(path.getFileName().toString(), ".json"))) &&
                                    !StringUtils.endsWith(path.getFileName().toString(), "-dev.yml") &&
                                    (Objects.equals(path.getParent().getFileName().toString(), "config") ||
                                            Objects.equals(path.getParent().getParent().getFileName().toString(), "config"))) // Chercher les fichiers nommés pom.xml
                    .filter(this::isNotIgnoredDirectory) // Ignorer les répertoires spécifiques
                    .collect(Collectors.toList());
        }
    }

    private boolean isConfig(Path path) {
        return StringUtils.startsWith(path.getFileName().toString(), "application")
                && (StringUtils.endsWith(path.getFileName().toString(), ".yml") ||
                StringUtils.endsWith(path.getFileName().toString(), ".properties"));
    }

    private boolean isNotIgnoredDirectory(Path path) {
        // Vérifier si le chemin contient "target" ou "node_modules" comme nom de répertoire
        // Cela permet de s'assurer que même si un pom.xml se trouve dans un sous-sous-répertoire d'un répertoire ignoré, il est bien ignoré.
        for (Path segment : path) {
            String segmentName = segment.getFileName().toString();
            if (Objects.equals(segmentName, "target") || Objects.equals(segmentName, "node_modules")
                    || Objects.equals(segmentName, "node")) {
                return false; // Le chemin contient un répertoire à ignorer
            }
        }
        return true; // Le chemin ne contient pas de répertoire à ignorer
    }

    private List<String> getListPath(File rep, String oldCommitId, String newCommitId) throws Exception {
        List<String> liste = new ArrayList<>();
        try (Git git = Git.open(rep)) {
            Repository repository = git.getRepository();

            List<DiffEntry> diffs = getDiffs(repository, oldCommitId, newCommitId);

            for (DiffEntry diff : diffs) {
                LOGGER.debug("Type : {}", diff.getChangeType());

                switch (diff.getChangeType()) {
                    case DELETE:
                        LOGGER.debug("Path : {}", diff.getOldPath());
                        ajoute(liste, diff.getOldPath());
                        break;

                    case ADD:
                    case MODIFY:
                    case RENAME:
                    case COPY:
                    default:
                        LOGGER.debug("Path : {}", diff.getNewPath());
                        if (diff.getOldPath() != null && !diff.getOldPath().isBlank()) {
                            ajoute(liste, diff.getOldPath());
                        }
                        ajoute(liste, diff.getNewPath());
                        break;
                }
            }
            liste = liste.stream()
                    .sorted()
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return liste;
    }

    private void ajoute(List<String> liste, String path) {
        if (path != null && !path.isBlank()) {
            path = normalisePath(path);
            if (!liste.contains(path) && path.contains("src/main/java/resources/config/") &&
                    !path.endsWith("-dev.yml")) {
                liste.add(path);
            }
        }
    }

    public List<DiffEntry> getDiffs(
            Repository repository,
            String oldCommitId,
            String newCommitId
    ) throws Exception {

        ObjectId oldHead = repository.resolve(oldCommitId);
        ObjectId newHead = repository.resolve(newCommitId);

        try (
                RevWalk revWalk = new RevWalk(repository);
                ObjectReader reader = repository.newObjectReader();
                DiffFormatter diffFormatter =
                        new DiffFormatter(new ByteArrayOutputStream())
        ) {
            RevCommit oldCommit = revWalk.parseCommit(oldHead);
            RevCommit newCommit = revWalk.parseCommit(newHead);

            RevTree oldTree = oldCommit.getTree();
            RevTree newTree = newCommit.getTree();

            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldTree);

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, newTree);

            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

            return diffFormatter.scan(oldTreeIter, newTreeIter);
        }
    }

    private boolean isBinaryFile(byte[] buf) throws IOException {
        int maxBytes = Math.min(1024, buf.length); // on lit seulement le début du fichier
        byte[] buffer = new byte[maxBytes];

        try (ByteArrayInputStream fis = new ByteArrayInputStream(buf)) {
            int bytesRead = fis.read(buffer);

            if (bytesRead == -1) {
                return false; // fichier vide → considéré comme texte
            }

            int nonPrintable = 0;

            for (int i = 0; i < bytesRead; i++) {
                int b = buffer[i] & 0xFF;

                // caractères de contrôle autorisés : tab, LF, CR
                if (b == 9 || b == 10 || b == 13) {
                    continue;
                }

                // ASCII imprimable : 32 à 126
                if (b < 32 || b > 126) {
                    nonPrintable++;
                }
            }

            // si plus de 30% des caractères sont non imprimables → binaire
            return ((double) nonPrintable / bytesRead) > 0.3;
        }
    }

    private String normalisePath(String s){
        if(s==null) {
            return null;
        } else {
            return s.replace("\\", "/");
        }
    }

}
