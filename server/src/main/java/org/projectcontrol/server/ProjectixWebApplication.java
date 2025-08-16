package org.projectcontrol.server;

import org.projectcontrol.server.properties.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({ApplicationProperties.class})
@SpringBootApplication(scanBasePackages = {"org.projectcontrol.server", "org.projectcontrol.core"})
public class ProjectixWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectixWebApplication.class, args);
    }

}


