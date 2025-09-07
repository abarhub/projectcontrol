package org.projectcontrol.core.utils;

import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

public class FileFlux2 {

    public static Flux<Contexte> rechercherEtFusionner(Flux<FileFlux.Ligne> fluxLignes,
                                                       List<String> motifs, int avant, int apres,
                                                       CacheCriteresRecherche cacheCriteresRecherche) {
        return fluxLignes
                .buffer(avant + apres + 1, 1)
                .filter(window -> window.size() == avant + apres + 1)
                .flatMap(window -> {
                    FileFlux.Ligne ligneCentrale = window.get(avant);
                    String contenu = ligneCentrale.contenu();

                    List<String> motifsTrouves = motifs.stream()
                            //.filter(contenu::contains)
                            .filter(cacheCriteresRecherche::contientTexte)
                            .collect(Collectors.toList());
                    boolean trouve = cacheCriteresRecherche.contientTexte(contenu);

//                    if (motifsTrouves.isEmpty()) {
                    if (!trouve) {
                        return Flux.empty();
                    }

                    List<LigneGrep> bloc = new ArrayList<>();
                    int n = 0;
                    for (FileFlux.Ligne l : window) {
                        boolean trouve2 = n == avant;
                        LigneGrep ligneGrep = convertie(l, trouve2, cacheCriteresRecherche);
                        bloc.add(ligneGrep);
                        n++;
                    }

                    int ligneDebut = window.get(0).numero();
                    int ligneFin = window.get(window.size() - 1).numero();

                    return Flux.just(new Contexte(ligneDebut, ligneFin,
                            new HashSet<>(motifsTrouves), bloc));
                })
                .collectList()          // rassembler tous les blocs
                .flatMapMany(FileFlux2::fusionnerBlocs);
    }

    private static LigneGrep convertie(FileFlux.Ligne l, boolean trouve2, CacheCriteresRecherche cacheCriteresRecherche) {
        LigneGrep ligneGrep = new LigneGrep();
        ligneGrep.setNoLigne(l.numero());
        ligneGrep.setLigne(l.contenu());
        ligneGrep.setTrouve(trouve2);
        if(trouve2) {
            ligneGrep.setRange(cacheCriteresRecherche.getPositionsTexte(l.contenu()));
        }
        return ligneGrep;
    }

    private static Flux<Contexte> fusionnerBlocs(List<Contexte> blocs) {
        if (blocs.isEmpty()) return Flux.empty();

        // trier par ligneDebut
        blocs.sort(Comparator.comparingInt(Contexte::getLigneDebut));

        List<Contexte> fusionnes = new ArrayList<>();
        Contexte courant = blocs.get(0);

        for (int i = 1; i < blocs.size(); i++) {
            Contexte suivant = blocs.get(i);

            if (suivant.getLigneDebut() <= courant.getLigneFin()) {
                // chevauchement -> fusion
                int ligneDebut = Math.min(courant.getLigneDebut(), suivant.getLigneDebut());
                int ligneFin = Math.max(courant.getLigneFin(), suivant.getLigneFin());

                Set<String> motifs = new HashSet<>(courant.getMotifs());
                motifs.addAll(suivant.getMotifs());

                List<LigneGrep> lignes = new ArrayList<>(courant.getLignes());
                for (LigneGrep l : suivant.getLignes()) {
                    var contient = lignes.stream().anyMatch(x -> x.getNoLigne() == l.getNoLigne());
                    if (!contient) {
                        lignes.add(l);
                    }
                }

                courant = new Contexte(ligneDebut, ligneFin, motifs, lignes);
            } else {
                fusionnes.add(courant);
                courant = suivant;
            }
        }
        fusionnes.add(courant);

        return Flux.fromIterable(fusionnes);
    }

    static public class Contexte {
        private final int ligneDebut;
        private final int ligneFin;
        private final Set<String> motifs;
        private final List<LigneGrep> lignes;

        public Contexte(int ligneDebut, int ligneFin, Set<String> motifs, List<LigneGrep> lignes) {
            this.ligneDebut = ligneDebut;
            this.ligneFin = ligneFin;
            this.motifs = motifs;
            this.lignes = lignes;
        }

        public int getLigneDebut() {
            return ligneDebut;
        }

        public int getLigneFin() {
            return ligneFin;
        }

        public Set<String> getMotifs() {
            return motifs;
        }

        public List<LigneGrep> getLignes() {
            return lignes;
        }

        @Override
        public String toString() {
            return "Contexte{lignes=" + ligneDebut + "-" + ligneFin +
                    ", motifs=" + motifs +
                    ", contenu=" + lignes + "}";
        }
    }
}
