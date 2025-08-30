package org.projectcontrol.cli.cli;

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
    private final SearchCli searchCli;
    private final MailCommand mailCommand;
    private final TopLevelCommand topLevelCommand;

    private int exitCode;

    public ApplicationRunner(UpdateVersionCli updateVersionCli, IFactory factory, SearchCli searchCli, MailCommand mailCommand, TopLevelCommand topLevelCommand) {
        this.updateVersionCli = updateVersionCli;
        this.factory = factory;
        this.searchCli = searchCli;
        this.mailCommand = mailCommand;
        this.topLevelCommand = topLevelCommand;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("run args:{}", args);
        // let picocli parse command line args and run the business logic
        //exitCode = new CommandLine(updateVersionCli, factory).execute(args);

        exitCode = new CommandLine(topLevelCommand, factory).execute(args);

//        try {
//            if (args.length > 0) {
//                if (args[0].equals("updateVersion")) {
//                    exitCode = updateVersionCli.call();
//                } else if (args[0].equals("search")) {
//                    String chemin=args[1];
//                    //exitCode=searchCli.call();
//                }
//            }
//        }catch (Exception e) {
//            LOGGER.error("error", e);
//            exitCode = 1;
//        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

}
