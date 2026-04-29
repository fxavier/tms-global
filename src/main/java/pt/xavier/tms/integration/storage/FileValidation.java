package pt.xavier.tms.integration.storage;

import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import pt.xavier.tms.shared.exception.BusinessException;

final class FileValidation {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private FileValidation() {
    }

    static void validate(MultipartFile file, long maxFileSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException("FILE_TOO_LARGE", "File cannot exceed 10 MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE", "Only PDF, JPG and PNG files are supported");
        }
    }
}
