package org.projectcontrol.core.service;

import org.projectcontrol.core.vo.projet.MavenDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class DependencyTreeParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTreeParserService.class);

    private final RunService runService = new RunService();

    /**
     * Lance "mvn dependency:tree -DoutputType=tgf" et retourne
     * la liste des dépendances DIRECTES enrichies de leurs transitives.
     */
    public List<MavenDependency> buildDependencyTree(Path projectDir, String mvnCmd) throws Exception {
        Path tgfFile = Files.createTempFile("dep-tree-", ".tgf");

        Path pomFile = projectDir.resolve("pom.xml").toAbsolutePath().normalize();

        try {
            // Lancer mvn dependency:tree en format TGF
            runProcess(projectDir, mvnCmd,
                    "dependency:tree",
                    "-DoutputType=tgf",
                    "-Doutput=" + tgfFile.toString(),
                    "--batch-mode",
                    "--no-transfer-progress",
                    "-f=" + pomFile
            );

            return parseTgf(tgfFile);
        } finally {
            Files.deleteIfExists(tgfFile);
        }
    }

    // ---------------------------------------------------------------
    // Parser le fichier TGF
    // ---------------------------------------------------------------
    private List<MavenDependency> parseTgf(Path tgfFile) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(tgfFile)) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line.trim());
        }

        // Séparer la section "noeuds" et la section "arêtes" (séparées par "#")
        int separatorIdx = lines.indexOf("#");
        if (separatorIdx < 0) return Collections.emptyList();

        List<String> nodeLines = lines.subList(0, separatorIdx);
        List<String> edgeLines = lines.subList(separatorIdx + 1, lines.size());

        // 1. Construire la map id → Dependency
        Map<String, MavenDependency> nodeMap = new LinkedHashMap<>();
        String rootId = null;

        for (String line : nodeLines) {
            if (line.isEmpty()) continue;
            // Format : "1\tgroupId:artifactId:type:version:scope"
            int tabIdx = line.indexOf('\t');
            if (tabIdx < 0) continue;

            String id = line.substring(0, tabIdx).trim();
            String depStr = line.substring(tabIdx + 1).trim();
            MavenDependency dep = parseDependencyString(depStr);

            nodeMap.put(id, dep);
            if (rootId == null) rootId = id; // le premier nœud = le projet lui-même
        }

        // 2. Construire l'arbre via les arêtes
        //    Format : "1\t2"  signifie que le nœud 1 a pour enfant le nœud 2
        for (String edge : edgeLines) {
            if (edge.isEmpty()) continue;
            String[] parts = edge.split("\\t");
            if (parts.length < 2) continue;

            String parentId = parts[0].trim();
            String childId = parts[1].trim();

            MavenDependency parent = nodeMap.get(parentId);
            MavenDependency child = nodeMap.get(childId);
            if (parent != null && child != null) {
                parent.addTransitive(child);
            }
        }

        // 3. Retourner uniquement les dépendances directes du projet racine
        //    (les enfants directs du nœud racine)
        if (rootId == null) return Collections.emptyList();
        return nodeMap.get(rootId).getTransitiveDependencies();
    }

    /**
     * Parse une chaîne "groupId:artifactId:type:version:scope"
     */
    private MavenDependency parseDependencyString(String depStr) {
        String[] parts = depStr.split(":");
        // Certains artifacts ont un classifier : g:a:type:classifier:version:scope
        String groupId = parts.length > 0 ? parts[0] : "";
        String artifactId = parts.length > 1 ? parts[1] : "";
        String type = parts.length > 2 ? parts[2] : "jar";
        String version = parts.length > 3 ? parts[3] : "";
        String scope = parts.length > 4 ? parts[4] : "compile";

        // Si 6 parties → classifier présent, décaler
        if (parts.length == 6) {
            // g:a:type:classifier:version:scope
            type = parts[2];
            // parts[3] = classifier (ignoré ici, à ajouter si besoin)
            version = parts[4];
            scope = parts[5];
        }

        return new MavenDependency(groupId, artifactId, version, type, scope, false);
    }

    // ---------------------------------------------------------------
    // Utilitaire : lancer un process Maven
    // ---------------------------------------------------------------
    void runProcess(Path dir, String mvnCmd, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(mvnCmd);
        cmd.addAll(Arrays.asList(args));
        int exitCode = runService.runCommand(x -> LOGGER.debug("[MVN] {}", x),
                x -> {
                    if (x.line().startsWith("WARNING: ") || x.line().isEmpty()) {
                        LOGGER.debug("[MVN] {}", x);
                    } else {
                        LOGGER.error("[MVN] {}", x);
                    }
                },
                dir, cmd.toArray(String[]::new));
        if (exitCode != 0) {
            throw new RuntimeException("Maven a échoué (exit=" + exitCode + ") : " + Arrays.toString(args));
        }
    }

}
