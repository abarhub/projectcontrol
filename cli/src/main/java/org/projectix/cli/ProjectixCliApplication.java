package org.projectix.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectixCliApplication {

    public static void main(String[] args) {
        //SpringApplication.run(ProjectixCliApplication.class, args);

//        System.exit(SpringApplication.exit(SpringApplication.run(ProjectixCliApplication.class, args)));

        SpringApplication application = new SpringApplication(ProjectixCliApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(application.run(args)));
    }

}


