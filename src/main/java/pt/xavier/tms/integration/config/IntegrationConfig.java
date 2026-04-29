package pt.xavier.tms.integration.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties({FileStorageConfig.class, RhIntegrationConfig.class})
public class IntegrationConfig {

    @Bean
    @ConditionalOnProperty(name = "tms.storage.type", havingValue = "s3")
    S3Client s3Client() {
        return S3Client.create();
    }
}
