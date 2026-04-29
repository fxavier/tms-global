package pt.xavier.tms.integration.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import pt.xavier.tms.integration.config.FileStorageConfig;
import pt.xavier.tms.integration.dto.FileUploadResultDto;
import pt.xavier.tms.shared.exception.BusinessException;

class LocalFileStorageAdapterTests {

    @TempDir
    private Path tempDir;

    @Test
    void uploadStoresAllowedFileAndDownloadReturnsResource() throws Exception {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(config(1024));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "sample".getBytes()
        );

        FileUploadResultDto result = adapter.upload(file);
        Resource resource = adapter.download(result.storageKey());

        assertThat(result.originalFilename()).isEqualTo("document.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.sizeBytes()).isEqualTo(6);
        assertThat(result.storageKey()).endsWith(".pdf");
        assertThat(resource.exists()).isTrue();
        assertThat(Files.readString(resource.getFile().toPath())).isEqualTo("sample");
    }

    @Test
    void uploadRejectsFilesOverConfiguredLimit() {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(config(3));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "sample".getBytes()
        );

        assertThatThrownBy(() -> adapter.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File cannot exceed 10 MB");
    }

    @Test
    void uploadRejectsUnsupportedContentType() {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(config(1024));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.txt",
                "text/plain",
                "sample".getBytes()
        );

        assertThatThrownBy(() -> adapter.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only PDF, JPG and PNG files are supported");
    }

    private FileStorageConfig config(long maxFileSizeBytes) {
        return new FileStorageConfig(
                "local",
                maxFileSizeBytes,
                new FileStorageConfig.Local(tempDir),
                new FileStorageConfig.S3(null, "")
        );
    }
}
