package org.projectcontrol.cli.cli;

import org.springframework.stereotype.Service;
import picocli.CommandLine;

@Service
@CommandLine.Command(subcommands = {
        SearchCli.class,
        MailCommand.class,
        UpdateVersionCli.class,
        CommandLine.HelpCommand.class
})
public class TopLevelCommand {

}
