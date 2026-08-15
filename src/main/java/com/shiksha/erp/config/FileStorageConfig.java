package com.shiksha.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class FileStorageConfig {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Bean
    public CommandLineRunner createUploadDirectoryRunner() {
        return args -> {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.info("Uploads directory created at: {}", dir.getAbsolutePath());
                }
            } else {
                log.info("Uploads directory ready at: {}", dir.getAbsolutePath());
            }
        };
    }
}
