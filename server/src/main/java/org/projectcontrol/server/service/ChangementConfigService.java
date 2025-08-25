package org.projectcontrol.server.service;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

        StringBuilder res=new StringBuilder();

        // Construire le repository Git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

        Git git = new Git(repository);

        // Récupérer les contenus des fichiers pour chaque commit
        String yamlContent1 = getFileContentFromCommit(repository, commit1Hash, yamlFilePath);
        String yamlContent2 = getFileContentFromCommit(repository, commit2Hash, yamlFilePath);

        // Parser les YAML
        Yaml yaml = new Yaml();
        Map<Object, Object> parsedYaml1 = yaml.load(yamlContent1);
        Map<Object, Object> parsedYaml2 = yaml.load(yamlContent2);

        Path tempDir= Files.createTempDirectory("cmpyml");
        // Fichiers de sortie
        String outputFile1 = tempDir+"/flattened_" + commit1Hash + ".yml";
        String outputFile2 = tempDir+"/flattened_" + commit2Hash + ".yml";

        // Écrire les fichiers YAML aplatis
        writeFlattenedYaml(parsedYaml1, outputFile1);
        writeFlattenedYaml(parsedYaml2, outputFile2);

        // Comparer les fichiers
        compareFiles(outputFile1, outputFile2, res);

        Files.delete(Paths.get(outputFile1));
        Files.delete(Paths.get(outputFile2));
        Files.deleteIfExists(tempDir);

        repository.close();

        return res.toString();
    }

    private String getFileContentFromCommit(Repository repository, String commitHash, String filePath) throws Exception {
        ObjectId commitId = repository.resolve(commitHash);
        RevCommit commit = new org.eclipse.jgit.revwalk.RevWalk(repository).parseCommit(commitId);

        RevTree tree=commit.getTree();

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file '"+filePath+"'");
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // and then one can the loader to read the file
            loader.copyTo(outputStream);

            return outputStream.toString(StandardCharsets.UTF_8);
        }

//        return new String(
//                repository.open(
//                        commit.getTree().toObjectId()findBlobMemberPath(filePath)
//                ).getBytes()
//        );
    }

    private void writeFlattenedYaml(Map<Object, Object> data, String outputFile) throws IOException {
        Map<Object, Object> flattenedMap = new TreeMap<>();
        flattenMap(data, "", flattenedMap);

        try (FileWriter writer = new FileWriter(outputFile)) {
            for (Map.Entry<Object, Object> entry : flattenedMap.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
        }
    }

    private void flattenMap(Map<Object, Object> source, String prefix, Map<Object, Object> destination) {
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? ""+entry.getKey() : prefix + "." + entry.getKey();

            if (entry.getValue() instanceof Map) {
                flattenMap((Map<Object, Object>)entry.getValue(), key, destination);
            } else {
                destination.put(key, entry.getValue());
            }
        }
    }

    private void compareFiles(String file1, String file2, StringBuilder res) throws IOException {
        Map<String,String> map1=readFile(Paths.get(file1));
        Map<String,String> map2=readFile(Paths.get(file2));

        Set<String> keysAjout=new TreeSet<>(map2.keySet());
        keysAjout.removeAll(map1.keySet());

        Set<String> keysSupprime=new TreeSet<>(map1.keySet());
        keysSupprime.removeAll(map2.keySet());

        Set<String> keysModifie=new TreeSet<>();

        for (String key : map2.keySet()) {
            if (map1.containsKey(key)) {
                if(!Objects.equals(map1.get(key), map2.get(key))){
                    keysModifie.add(key);
                }
            }
        }

        res.append("* Parametre à ajouter : \n");
        for(String key : keysAjout){
            res.append(key).append(": ").append(str(map2.get(key))).append("\n");
        }
        res.append("* Parametre à modifier : \n");
        for(String key : keysModifie){
            res.append(key).append(": ").append(str(map2.get(key))).append("\n");
        }
        res.append("* Parametre à supprimer : \n");
        for(String key : keysSupprime){
            res.append(key).append("\n");
        }
    }

    private String str(String str) {
        if(str==null) {
            return "";
        } else {
            return str;
        }
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

    private Map<String,String> readFile(Path path) throws IOException {
        List<String> liste=Files.readAllLines(path);

        Map<String,String> map=new HashMap<>();

        for(String line:liste) {
            int index=line.indexOf(':');
            map.put(line.substring(0,index),line.substring(index+2));
        }

        return map;
    }


    public String calculDifference(Path file, String commitDebut, String commitFin) throws Exception {

        ChangementConfigService changeConfig=this;

        Path root=file;
        LOGGER.info("file={}",file);
        LOGGER.info("root={}",root);

        List<Path> liste=findConfigFiles(root.toString());

        StringBuilder sb=new StringBuilder();
        for(Path p:liste){
            var f=root.relativize(p);
            LOGGER.info("analyse de : {}",f);
            var s=f.toString();
            s=s.replaceAll("\\\\", "/");
            sb.append("*** Analyse de : ").append(s).append(" ***\n");
            sb.append(changeConfig.compareYamlFiles(root.toString(),commitDebut,commitFin,s));
        }

        return sb.toString();

    }

    public List<Path> findConfigFiles(String directoryPath) throws IOException {
        Path startPath = Paths.get(directoryPath);

        try (Stream<Path> walk = Files.walk(startPath)) {
            return walk
                    .filter(Files::isRegularFile) // Ne traiter que les fichiers réguliers
                    .filter(path -> StringUtils.startsWith(path.getFileName().toString(),"application")
                            &&StringUtils.endsWith(path.getFileName().toString(),".yml")
                            &&!StringUtils.endsWith(path.getFileName().toString(),"-dev.yml")
                            && Objects.equals(path.getParent().getFileName().toString(), "config")) // Chercher les fichiers nommés pom.xml
                    .filter(ChangementConfigService::isNotIgnoredDirectory) // Ignorer les répertoires spécifiques
                    .collect(Collectors.toList());
        }
    }


    private static boolean isNotIgnoredDirectory(Path path) {
        // Vérifier si le chemin contient "target" ou "node_modules" comme nom de répertoire
        // Cela permet de s'assurer que même si un pom.xml se trouve dans un sous-sous-répertoire d'un répertoire ignoré, il est bien ignoré.
        for (Path segment : path) {
            String segmentName = segment.getFileName().toString();
            if (Objects.equals(segmentName,"target") || Objects.equals(segmentName,"node_modules")
                    || Objects.equals(segmentName,"node")) {
                return false; // Le chemin contient un répertoire à ignorer
            }
        }
        return true; // Le chemin ne contient pas de répertoire à ignorer
    }

}
