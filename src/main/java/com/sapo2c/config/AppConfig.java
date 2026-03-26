package com.sapo2c.config;

import com.sapo2c.service.DataIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final DataIngestionService ingestionService;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CommandLineRunner startupRunner() {
        return args -> {
            log.info("=== SAP O2C Graph System Starting ===");

            if (!ingestionService.isDataLoaded()) {
                log.info("No data found — starting ingestion...");
                ingestionService.ingestAllData();
            } else {
                log.info("Data already loaded, skipping ingestion.");
            }

            if (!ingestionService.isGraphBuilt()) {
                log.info("Graph not built — building now...");
                ingestionService.buildGraph();
            } else {
                log.info("Graph already built, skipping.");
            }

            log.info("=== System Ready ===");
        };
    }
}
