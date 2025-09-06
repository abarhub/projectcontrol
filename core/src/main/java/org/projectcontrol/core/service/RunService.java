package org.projectcontrol.core.service;


import org.projectcontrol.core.utils.LignesRecherche;
import org.projectcontrol.core.utils.Line;
import org.projectcontrol.core.utils.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RunService {


    private static final Logger LOGGER = LoggerFactory.getLogger(RunService.class);

    public int runCommand(Consumer<Line> consumer, String... commandes) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> liste = new ArrayList<>();
        for (String s : commandes) {
            if (s.contains(" ")) {
                liste.add("\"" + s + "\"");
            } else {
                liste.add(s);
            }
        }
        Sinks.Many<LignesRecherche> sink = Sinks.many().multicast().onBackpressureBuffer();

        Flux<LignesRecherche> hotFlux = sink.asFlux();
        LOGGER.info("run {}", liste);
        builder.command(liste);
        Process process = builder.start();
        try (ExecutorService executorService = Executors.newCachedThreadPool()) {
            //LOGGER.info("output: {}",x);
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), x -> consumer.accept(new Line(false, x)));
            executorService.submit(streamGobbler);
            StreamGobbler streamGobblerErrur =
                    new StreamGobbler(process.getErrorStream(), (x) -> {
                        LOGGER.error("error: {}", x);
                        consumer.accept(new Line(true, x));
                    });
            executorService.submit(streamGobblerErrur);
            return process.waitFor();
        }
    }

    public Flux<Line> runCommand(String... commandes) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> liste = new ArrayList<>();
        for (String s : commandes) {
            if (s.contains(" ")) {
                liste.add("\"" + s + "\"");
            } else {
                liste.add(s);
            }
        }
        Sinks.Many<Line> sink = Sinks.many().multicast().onBackpressureBuffer();

        Flux<Line> hotFlux = sink.asFlux();
        LOGGER.info("run {}", liste);
        builder.command(liste);
        Process process = builder.start();
        try (ExecutorService executorService = Executors.newCachedThreadPool()) {
            //LOGGER.info("output: {}",x);
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), x -> sink.emitNext(new Line(false, x), Sinks.EmitFailureHandler.FAIL_FAST));
            executorService.submit(streamGobbler);
            StreamGobbler streamGobblerErrur =
                    new StreamGobbler(process.getErrorStream(), (x) -> {
                        LOGGER.error("error: {}", x);
                        sink.emitNext(new Line(true, x), Sinks.EmitFailureHandler.FAIL_FAST);
                    });
            executorService.submit(streamGobblerErrur);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                sink.emitError(new RuntimeException("exit code : " + exitCode), Sinks.EmitFailureHandler.FAIL_FAST);
            }
        } finally {
            sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        }
        return hotFlux;
    }
}
