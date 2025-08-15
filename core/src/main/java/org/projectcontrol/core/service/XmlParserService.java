package org.projectcontrol.core.service;


import org.projectcontrol.core.vo.Position;
import org.projectcontrol.core.vo.PositionIndex;
import org.projectcontrol.core.vo.ResultatBalise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class XmlParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlParserService.class);

    public List<ResultatBalise> parse(Path fichier, List<List<String>> balisesRecherche) throws Exception {
        Path inputFile = fichier;
        List<ResultatBalise> resultat = new ArrayList<>();

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        LOGGER.info("Parsing du fichier {}", inputFile);

        try (var inputStream = Files.newInputStream(inputFile)) {
            XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);

            List<String> balises = new ArrayList<>();

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartElement()) {
                    String tagName = event.asStartElement().getName().getLocalPart();

                    balises.add(tagName);
                } else if (event.isCharacters() && contient(balisesRecherche, balises)) {
                    String originalText = event.asCharacters().getData();

                    LOGGER.info("Texte original dans <node2> : {},({})", originalText, event.getLocation());
                    Position debut, fin;
                    debut = new Position(event.getLocation().getLineNumber(),
                            event.getLocation().getColumnNumber()-originalText.length()-2,
                            event.getLocation().getCharacterOffset());
                    fin = new Position(event.getLocation().getLineNumber(),
                            event.getLocation().getColumnNumber()-3 /*+ originalText.length() - 1*/,
                            event.getLocation().getCharacterOffset() + (originalText.length() - 1));
                    resultat.add(new ResultatBalise(List.copyOf(balises), originalText, debut, fin));

                } else if (event.isEndElement()) {

                    balises.removeLast();
                }
            }

            reader.close();
        }

        LOGGER.info("nb resultat {}", resultat.size());

        return resultat;
    }

    public void modifierFichier(String cheminFichier, Position debut, Position fin, String nouveauTexte) throws IOException {
        // 1. Charger le fichier entier en tableau de caractères
        String contenu = Files.readString(Paths.get(cheminFichier));
        char[] caracteres = contenu.toCharArray();

        // 2. Parcourir le tableau pour trouver les positions de début et fin
//        PositionIndex posDebut = trouverPosition(caracteres, debut);
//        PositionIndex posFin = trouverPosition(caracteres, fin);
        int posDebut=debut.position();
        int posFin= fin.position();

        // 3. Valider que les positions ont été trouvées
        if (posDebut<=0) {
            throw new IllegalArgumentException("Position de début " + debut + " non trouvée dans le fichier " + cheminFichier);
        }
        if (posFin<=0) {
            throw new IllegalArgumentException("Position de fin " + fin + " non trouvée dans le fichier " + cheminFichier);
        }
        if (posDebut > posFin) {
            throw new IllegalArgumentException("La position de début doit être avant la position de fin " + cheminFichier);
        }

        // 4. Remplacer en mémoire dans le tableau de caractères
        char[] nouveauContenu = remplacerDansTableau(caracteres, posDebut, posFin, nouveauTexte);

        // 5. Écrire le nouveau contenu dans le fichier
        Files.writeString(Paths.get(cheminFichier), new String(nouveauContenu));

        LOGGER.info("Remplacement effectué :");
        LOGGER.info("- Position début: " + debut + " (index " + posDebut + ")");
        LOGGER.info("- Position fin: " + fin + " (index " + posFin + ")");
        LOGGER.info("- Texte remplacé par: \"" + nouveauTexte + "\"");
    }

    public void modifierFichier2(String cheminFichier, Position debut, Position fin, String nouveauTexte) throws IOException {
        // 1. Charger le fichier entier en tableau de caractères
        String contenu = Files.readString(Paths.get(cheminFichier));
        char[] caracteres = contenu.toCharArray();

        // 2. Parcourir le tableau pour trouver les positions de début et fin
        PositionIndex posDebut = trouverPosition(caracteres, debut);
        PositionIndex posFin = trouverPosition(caracteres, fin);
//        int posDebut=debut.position();
//        int posFin= fin.position();

        // 3. Valider que les positions ont été trouvées
        if (!posDebut.trouve()) {
            throw new IllegalArgumentException("Position de début " + debut + " non trouvée dans le fichier " + cheminFichier);
        }
        if (!posFin.trouve()) {
            throw new IllegalArgumentException("Position de fin " + fin + " non trouvée dans le fichier " + cheminFichier);
        }
        if (posDebut.index() > posFin.index()) {
            throw new IllegalArgumentException("La position de début doit être avant la position de fin " + cheminFichier);
        }

        // 4. Remplacer en mémoire dans le tableau de caractères
        char[] nouveauContenu = remplacerDansTableau(caracteres, posDebut.index(), posFin.index(), nouveauTexte);

        // 5. Écrire le nouveau contenu dans le fichier
        Files.writeString(Paths.get(cheminFichier), new String(nouveauContenu));

        LOGGER.info("Remplacement effectué :");
        LOGGER.info("- Position début: " + debut + " (index " + posDebut + ")");
        LOGGER.info("- Position fin: " + fin + " (index " + posFin + ")");
        LOGGER.info("- Texte remplacé par: \"" + nouveauTexte + "\"");
    }

    /**
     * Parcourt le tableau de caractères pour trouver l'index correspondant à la position ligne/colonne
     */
    private PositionIndex trouverPosition(char[] caracteres, Position position) {
        int ligneActuelle = 1;
        int colonneActuelle = 1;

        for (int i = 0; i < caracteres.length; i++) {
            // Vérifier si on a atteint la position recherchée
            if (ligneActuelle == position.ligne() && colonneActuelle == position.colonne()) {
                return new PositionIndex(i, true);
            }

            // Gérer les sauts de ligne
            if (caracteres[i] == '\n') {
                ligneActuelle++;
                colonneActuelle = 1;
            } else if (caracteres[i] == '\r') {
                // Gérer les retours chariot (Windows: \r\n, Mac classique: \r)
                if (i + 1 < caracteres.length && caracteres[i + 1] == '\n') {
                    // Windows: \r\n - on passe le \r, le \n sera traité au prochain tour
                    continue;
                } else {
                    // Mac classique: \r seul
                    ligneActuelle++;
                    colonneActuelle = 1;
                }
            } else {
                colonneActuelle++;
            }
        }

        // Vérifier si la position est à la fin du fichier
        if (ligneActuelle == position.ligne() && colonneActuelle == position.colonne()) {
            return new PositionIndex(caracteres.length, true);
        }

        return new PositionIndex(-1, false);
    }

    /**
     * Remplace la portion du tableau entre indexDebut et indexFin par le nouveau texte
     */
    private char[] remplacerDansTableau(char[] original, int indexDebut, int indexFin, String nouveauTexte) {
        char[] nouveauTexteChars = nouveauTexte.toCharArray();

        // Calculer la taille du nouveau tableau
        int tailleOriginale = original.length;
        int tailleASupprimer = indexFin - indexDebut + 1;
        int tailleAAjouter = nouveauTexteChars.length;
        int nouvelleTaille = tailleOriginale - tailleASupprimer + tailleAAjouter;

        // Créer le nouveau tableau
        char[] resultat = new char[nouvelleTaille];

        // Copier la partie avant le remplacement
        System.arraycopy(original, 0, resultat, 0, indexDebut);

        // Copier le nouveau texte
        System.arraycopy(nouveauTexteChars, 0, resultat, indexDebut, nouveauTexteChars.length);

        // Copier la partie après le remplacement
        System.arraycopy(original, indexFin + 1, resultat, indexDebut + nouveauTexteChars.length,
                tailleOriginale - indexFin - 1);

        return resultat;
    }

    private <T> boolean contient(List<List<T>> liste, List<T> element) {
        for (List<T> l : liste) {
            if (l.equals(element)) {
                return true;
            }
        }
        return false;
    }

}
