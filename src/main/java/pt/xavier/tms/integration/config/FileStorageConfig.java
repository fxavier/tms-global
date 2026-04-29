package pt.xavier.tms.integration.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tms.storage")
public record FileStorageConfig(
        String type,
        long maxFileSizeBytes,
        Local local,
        S3 s3
) {

    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    public FileStorageConfig {
        if (type == null || type.isBlank()) {
            type = "local";
        }
        if (maxFileSizeBytes < 1) {
            maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE_BYTES;
        }
        if (local == null) {
            local = new Local(Path.of("./storage/uploads"));
        }
        if (s3 == null) {
            s3 = new S3(null, "");
        }
    }

    public record Local(Path basePath) {

        public Local {
            if (basePath == null) {
                basePath = Path.of("./storage/uploads");
            }
        }
    }

    public record S3(String bucket, String prefix) {

        public S3 {
            if (prefix == null) {
                prefix = "";
            }
        }
    }
}
