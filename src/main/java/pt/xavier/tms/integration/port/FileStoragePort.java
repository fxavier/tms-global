package pt.xavier.tms.integration.port;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import pt.xavier.tms.integration.dto.FileUploadResultDto;

public interface FileStoragePort {

    FileUploadResultDto upload(MultipartFile file);

    Resource download(String storageKey);
}
