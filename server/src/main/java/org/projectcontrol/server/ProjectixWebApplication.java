package org.projectcontrol.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.projectcontrol.server","org.projectcontrol.core"})
public class ProjectixWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectixWebApplication.class, args);
    }

}


