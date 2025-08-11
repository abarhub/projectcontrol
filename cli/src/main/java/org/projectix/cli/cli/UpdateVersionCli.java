package org.projectix.cli.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "updateVersion")
public class UpdateVersionCli implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateVersionCli.class);

    public Integer call() throws Exception {
        //mailService.sendMessage(to, subject, String.join(" ", body));
        LOGGER.info("call");
        return 0;
    }

}
