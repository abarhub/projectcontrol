package org.projectcontrol.server.configuration;

import org.projectcontrol.core.service.RunService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Bean
    public RunService runService() {
        return new RunService();
    }

}
