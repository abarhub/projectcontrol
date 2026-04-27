package org.projectcontrol.core.service;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.projectcontrol.core.vo.projet.ArtifactInfo;
import org.projectcontrol.core.vo.projet.MavenDependency;
import org.projectcontrol.core.vo.projet.MavenProfile;
import org.projectcontrol.core.vo.projet.MavenProjet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EffectivePomReaderService {


    private static final Logger LOGGER = LoggerFactory.getLogger(EffectivePomReaderService.class);

    private final DependencyTreeParserService treeParser;

    public EffectivePomReaderService(DependencyTreeParserService treeParser) {
        this.treeParser = treeParser;
    }

    // ================================================================
    //  POINT D'ENTRÉE
    // ================================================================

    /**
     * Lit le effective-pom ET l'arbre de dépendances pour un projet Maven.
     *
     * @param pomFile Répertoire racine du projet
     * @return PomInfo complet (modules récursifs + dépendances transitives)
     */
    public MavenProjet readEffectivePom(Path pomFile) throws Exception {

        String mvnCmd = resolveMavenCommand(pomFile);

        // 1. Générer et parser le effective-pom
        Path effectivePomFile = null;
        MavenProjet info;
        try {
            effectivePomFile = runEffectivePom(pomFile.getParent(), mvnCmd);
            info = parseEffectivePom(effectivePomFile);
        } finally {
            if (effectivePomFile != null) {
                Files.deleteIfExists(effectivePomFile);
            }
        }
        info.setFichierMaven(pomFile.toAbsolutePath().normalize().toString());

        // 2. Enrichir les dépendances directes avec leurs transitives
        List<MavenDependency> enrichedDeps = treeParser.buildDependencyTree(pomFile.getParent(), mvnCmd);
        mergeTransitiveDeps(info.getDependencies(), enrichedDeps);

        // 3. Charger récursivement les modules
        loadModulesRecursively(info, pomFile, mvnCmd);

        return info;
    }

    // ================================================================
    //  ÉTAPE 1 : effective-pom
    // ================================================================
    private Path runEffectivePom(Path projectDir, String mvnCmd) throws Exception {
        Path outputFile = Files.createTempFile("effective-pom-", ".xml");

        Path pomFile = projectDir.resolve("pom.xml").toAbsolutePath().normalize();

        treeParser.runProcess(projectDir, mvnCmd,
                "help:effective-pom",
                "-Doutput=" + outputFile.toString(),
                "--batch-mode",
                "--no-transfer-progress",
                "-f=" + pomFile
        );

        if (Files.notExists(outputFile) || Files.size(outputFile) == 0) {
            throw new RuntimeException("effective-pom vide ou absent : " + outputFile);
        }
        return outputFile;
    }

    // ================================================================
    //  ÉTAPE 2 : Parser le XML → PomInfo
    // ================================================================

    private MavenProjet parseEffectivePom(Path xmlFile) throws Exception {
        // Lire la racine du XML pour détecter le cas multi-module
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream input = Files.newInputStream(xmlFile)) {
            Document doc = builder.parse(input);

            String rootTag = doc.getDocumentElement().getTagName();

            if ("project".equals(rootTag)) {
                // ── Cas simple : un seul <project> ──
                return parseSingleProject(xmlFile);

            } else if ("projects".equals(rootTag)) {
                // ── Cas multi-module : <projects><project>...<project>...</projects> ──
                return parseMultiModuleProjects(doc);

            } else {
                throw new RuntimeException("Racine XML inattendue : <" + rootTag + ">");
            }
        }
    }

    // ---------------------------------------------------------------
    // Cas 1 : un seul <project>
    // ---------------------------------------------------------------
    private MavenProjet parseSingleProject(Path xmlFile) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileReader fr = new FileReader(xmlFile.toFile())) {
            return buildPomInfo(reader.read(fr));
        }
    }

    // ---------------------------------------------------------------
    // Cas 2 : <projects> avec plusieurs <project>
    //   → le premier <project> = le parent/agrégateur
    //   → les suivants = les modules
    // ---------------------------------------------------------------
    private MavenProjet parseMultiModuleProjects(Document doc) throws Exception {
        NodeList projectNodes = doc.getDocumentElement()
                .getElementsByTagName("project");

        if (projectNodes.getLength() == 0) {
            throw new RuntimeException("Aucun <project> trouvé dans <projects>");
        }

        MavenXpp3Reader reader = new MavenXpp3Reader();

        // -- Parser chaque <project> individuellement --
        List<MavenProjet> allModules = new ArrayList<>();
        for (int i = 0; i < projectNodes.getLength(); i++) {
            Node projectNode = projectNodes.item(i);
            String projectXml = nodeToXmlString(projectNode);

            try (StringReader sr = new StringReader(projectXml)) {
                Model model = reader.read(sr);
                allModules.add(buildPomInfo(model));
            }
        }

        // -- Le premier = projet racine (l'agrégateur) --
        MavenProjet root = allModules.get(0);

        // -- Les suivants = modules, on les attache au root --
        // (ils remplacent les stubs créés dans buildPomInfo)
        if (allModules.size() > 1) {
            root.getModules().clear();
            root.getModules().addAll(allModules.subList(1, allModules.size()));
        }

        return root;
    }

    // ---------------------------------------------------------------
    // Utilitaire : convertir un nœud DOM en String XML
    // ---------------------------------------------------------------
    private String nodeToXmlString(Node node) throws TransformerException {
        var factory = TransformerFactory.newInstance();
        // Désactive l'accès aux DTD externes
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        // Désactive l'accès aux stylesheets externes (XSL externes)
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        // Active le mode secure processing
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(sw));
        return sw.toString();
    }

    private MavenProjet buildPomInfo(Model model) {
        MavenProjet info = new MavenProjet();

        // Parent
        if (model.getParent() != null) {
            Parent p = model.getParent();
            info.setParent(new ArtifactInfo(
                    p.getGroupId(), p.getArtifactId(), p.getVersion(), null));
        }

        // Artifact courant (hérite groupId/version du parent si absent)
        String groupId = coalesce(model.getGroupId(),
                model.getParent() != null ? model.getParent().getGroupId() : "");
        String version = coalesce(model.getVersion(),
                model.getParent() != null ? model.getParent().getVersion() : "");
        info.setArtifact(new ArtifactInfo(
                groupId, model.getArtifactId(), version, model.getPackaging()));

        // Nom & Description
        info.setName(model.getName());
        info.setDescription(model.getDescription());

        // Propriétés
        if (model.getProperties() != null) {
            Map<String, String> props = new LinkedHashMap<>();
            model.getProperties().forEach((k, v) -> props.put(k.toString(), v.toString()));
            info.setProperties(props);
        }

        // Dépendances directes (sans transitives pour l'instant)
        for (org.apache.maven.model.Dependency dep : model.getDependencies()) {
            info.getDependencies().add(new MavenDependency(
                    dep.getGroupId(),
                    dep.getArtifactId(),
                    dep.getVersion(),
                    dep.getType(),
                    dep.getScope(),
                    Boolean.parseBoolean(dep.getOptional())
            ));
        }

        // Modules : on stocke juste les noms pour l'instant
        // (remplis récursivement dans loadModulesRecursively)
        for (String moduleName : model.getModules()) {
            MavenProjet moduleInfo = new MavenProjet();
            moduleInfo.setName(moduleName);
            info.getModules().add(moduleInfo);
        }

        // Profiles
        for (org.apache.maven.model.Profile profile : model.getProfiles()) {
            MavenProfile p = new MavenProfile(profile.getId());
            if (profile.getProperties() != null) {
                profile.getProperties().forEach((k, v) ->
                        p.getProperties().put(k.toString(), v.toString()));
            }
            for (org.apache.maven.model.Dependency dep : profile.getDependencies()) {
                p.getDependencies().add(new MavenDependency(
                        dep.getGroupId(), dep.getArtifactId(),
                        dep.getVersion(), dep.getType(),
                        dep.getScope(), Boolean.parseBoolean(dep.getOptional())
                ));
            }
            info.getProfiles().add(p);
        }

        return info;
    }

    // ================================================================
    //  ÉTAPE 3 : Charger les modules récursivement
    // ================================================================
    private void loadModulesRecursively(MavenProjet parentInfo,
                                        Path parentDir,
                                        String mvnCmd) {
        List<MavenProjet> resolved = new ArrayList<>();

        for (MavenProjet stub : parentInfo.getModules()) {

            // ✅ Si le module est déjà complet (artifact rempli), pas besoin de le recharger
            if (stub.getArtifact() != null && stub.getArtifact().getArtifactId() != null) {
                resolved.add(stub);
                continue;
            }

            // Sinon, on tente de le charger depuis le sous-répertoire (fallback)
            Path moduleDir = parentDir.resolve(stub.getName());
            if (!Files.notExists(moduleDir) || !Files.isDirectory(moduleDir)) {
                LOGGER.error("[WARN] Module introuvable : {}", moduleDir);
                resolved.add(stub);
                continue;
            }

            try {
                LOGGER.info("[INFO] Chargement du module : {}", moduleDir.getFileName());
                MavenProjet moduleInfo = readEffectivePom(moduleDir);
                resolved.add(moduleInfo);
            } catch (Exception e) {
                LOGGER.error("[WARN] Erreur module {} : {}", stub.getName(), e.getMessage());
                resolved.add(stub);
            }
        }

        parentInfo.getModules().clear();
        parentInfo.getModules().addAll(resolved);
    }

    // ================================================================
    //  ÉTAPE 4 : Fusionner les dépendances directes et transitives
    // ================================================================

    /**
     * On a les dépendances directes issues du effective-pom (avec optional, etc.)
     * et l'arbre complet issu de dependency:tree.
     * On fusionne pour enrichir chaque dépendance directe avec ses transitives.
     */
    private void mergeTransitiveDeps(List<MavenDependency> direct, List<MavenDependency> fromTree) {
        // Index de l'arbre par groupId:artifactId pour la recherche
        Map<String, MavenDependency> treeIndex = new LinkedHashMap<>();
        for (MavenDependency d : fromTree) {
            treeIndex.put(d.getGroupId() + ":" + d.getArtifactId(), d);
        }

        for (MavenDependency d : direct) {
            String key = d.getGroupId() + ":" + d.getArtifactId();
            MavenDependency treeNode = treeIndex.get(key);
            if (treeNode != null) {
                // Copier les sous-dépendances transitives
                treeNode.getTransitiveDependencies()
                        .forEach(d::addTransitive);
            }
        }
    }

    // ================================================================
    //  Utilitaires
    // ================================================================
    private String resolveMavenCommand(Path projectDir) {
        File mvnw = new File(projectDir.toFile(), isWindows() ? "mvnw.cmd" : "mvnw");
        if (mvnw.exists() && mvnw.canExecute()) return mvnw.getAbsolutePath();
        return isWindows() ? "mvn.cmd" : "mvn";
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return "";
    }

}
