package org.projectcontrol.cli.cli;

import org.apache.commons.lang3.StringUtils;
import org.projectcontrol.core.service.PomParserService;
import org.projectcontrol.core.service.XmlParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "updateVersion")
public class UpdateVersionCli implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateVersionCli.class);

    private XmlParserService xmlParserService;
    private PomParserService pomParserService;

    public UpdateVersionCli(XmlParserService xmlParserService, PomParserService pomParserService) {
        this.xmlParserService = xmlParserService;
        this.pomParserService = pomParserService;
    }

    public Integer call() throws Exception {
        //mailService.sendMessage(to, subject, String.join(" ", body));
        try {
            LOGGER.info("call");
            afficheVersions();
            return 0;
        } catch (Exception e) {
            LOGGER.error("error", e);
            return 1;
        }
    }

    private void afficheVersions() throws Exception {
        Path pomFile = Path.of("./pom.xml").toAbsolutePath().normalize();
        System.out.println("analyse du fichier : " + pomFile);
        var resultat = xmlParserService.parse(pomFile, List.of(PomParserService.PROJET_VERSION));
        if (!CollectionUtils.isEmpty(resultat)) {
            var res = resultat.getFirst();
            affiche(res.valeur(), pomFile);
        }
    }

    private void affiche(String versionActuelle, Path pomFile) throws Exception {
        System.out.println("version actuelle : " + versionActuelle);
        List<String> listeVersions = getListeVersion(versionActuelle);
        List<String> liste2 = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        for (String version : listeVersions) {
            var libelle = "Version " + version;
            map.put(libelle, version);
            liste2.add(libelle);
        }
//        var console = System.console();
//        if (console == null) {
//            throw new Exception("console est null");
//        }
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.println("Versions disponibles :");
            System.out.println("=====================");
            int i = 1;
            for (String libelle : liste2) {
                System.out.println(i + " ) " + libelle);
                i++;
            }
            System.out.println("0 ) Quitter");

            System.out.println("Veullez sélectionner une version :");
//            String choixStr = console.readLine();
            String choixStr = scan.nextLine();
            if (StringUtils.isNotBlank(choixStr)) {
                int choix = Integer.parseInt(choixStr);
                if (choix == 0) {
                    break;
                } else if (choix > 0 && choix <= liste2.size()) {
                    var version = map.get(liste2.get(choix - 1));
                    pomParserService.updateVersion(pomFile, version);
                }
            }
        }

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

}
