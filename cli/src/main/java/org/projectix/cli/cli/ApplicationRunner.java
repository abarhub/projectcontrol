package org.projectix.cli.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;


@Component
public class ApplicationRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

    private final IFactory factory;
    private final UpdateVersionCli updateVersionCli;

    private int exitCode;

    public ApplicationRunner(UpdateVersionCli updateVersionCli, IFactory factory) {
        this.updateVersionCli = updateVersionCli;
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("run args:{}", args);
        // let picocli parse command line args and run the business logic
        //exitCode = new CommandLine(updateVersionCli, factory).execute(args);
        try {
            if (args.length > 0) {
                if (args[0].equals("updateVersion")) {
                    exitCode = updateVersionCli.call();
                }
            }
        }catch (Exception e) {
            LOGGER.error("error", e);
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

}
