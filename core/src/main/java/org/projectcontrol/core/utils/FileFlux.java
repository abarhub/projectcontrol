package org.projectcontrol.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FileFlux {


    private static final Logger LOGGER = LoggerFactory.getLogger(FileFlux.class);

    private FileFlux() {
    }

    public static Flux<Ligne> lire(String cheminFichier) {
        return Flux.generate(
                () -> {
                    try {
                        return new State(new BufferedReader(new FileReader(cheminFichier)));
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
                        int num = state.numLigne.incrementAndGet();
                        sink.next(new Ligne(num, ligne));
                    } catch (IOException e) {
                        sink.error(e);
                    }
                    return state;
                },
                state -> {
                    try {
                        state.reader.close();
                    } catch (IOException e) {
                        LOGGER.error("erreur pour fermer le flux (fichier:{})", cheminFichier, e);
                    }
                }
        );
    }

    public static class State {
        final BufferedReader reader;
        final AtomicInteger numLigne = new AtomicInteger(0);

        State(BufferedReader reader) {
            this.reader = reader;
        }
    }

    public record Ligne(int numero, String contenu) {
    }

    public static Flux<Contexte> rechercher(Flux<Ligne> fluxLignes,
                                            String motif, int avant, int apres,
                                            CacheCriteresRecherche cacheCriteresRecherche) {
        return fluxLignes
                .buffer(avant + apres + 1, 1) // fenêtre glissante
                .filter(window -> window.size() == avant + apres + 1) // ignorer début/fin
                //.filter(window -> window.get(avant).contenu().contains(motif)) // ligne centrale contient motif
                .filter(window -> cacheCriteresRecherche.contientTexte(window.get(avant).contenu())) // ligne centrale contient motif
                .map(window -> {
                    FileFlux.Ligne ligneTrouvee = window.get(avant);
                    List<String> bloc = new ArrayList<>();
                    for (FileFlux.Ligne l : window) {
                        bloc.add(l.numero() + ": " + l.contenu());
                    }
                    return new Contexte(ligneTrouvee.numero(), bloc);
                });
    }

    public static class Contexte {
        private final int ligneTrouvee;
        private final List<String> lignes;

        public Contexte(int ligneTrouvee, List<String> lignes) {
            this.ligneTrouvee = ligneTrouvee;
            this.lignes = lignes;
        }

        public int getLigneTrouvee() {
            return ligneTrouvee;
        }

        public List<String> getLignes() {
            return lignes;
        }

        @Override
        public String toString() {
            return "Contexte{ligneTrouvee=" + ligneTrouvee + ", lignes=" + lignes + "}";
        }
    }
}
