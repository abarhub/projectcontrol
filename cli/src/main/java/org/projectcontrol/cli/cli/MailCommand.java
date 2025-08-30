package org.projectcontrol.cli.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "mailCommand")
public class MailCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailCommand.class);

    @CommandLine.Option(names = "--to", description = "email(s) of recipient(s)", required = true)
    List<String> to;

    @CommandLine.Option(names = "--subject", description = "Subject")
    String subject;

    @CommandLine.Parameters(description = "Message to be sent")
    String[] body = {};

    public Integer call() throws Exception {
        //mailService.sendMessage(to, subject, String.join(" ", body));
        LOGGER.info("mail sent to:{} subject:{} body:{} ", to, subject, String.join(" ", body));
        return 0;
    }
}
