package com.ahadporkar.pruefpilot;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Entry point. A single deployable, deliberately bounded modular monolith. */
@SpringBootApplication
public class PruefPilotApplication {
    public static void main(String[] args) { SpringApplication.run(PruefPilotApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
