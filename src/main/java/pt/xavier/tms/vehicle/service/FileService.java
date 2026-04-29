package pt.xavier.tms.vehicle.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pt.xavier.tms.integration.dto.FileUploadResultDto;
import pt.xavier.tms.integration.port.FileStoragePort;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.vehicle.entity.FileRecord;
import pt.xavier.tms.vehicle.repository.FileRecordRepository;

@Service
@ConditionalOnProperty(name = "tms.vehicle.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class FileService {

    private final FileStoragePort fileStoragePort;
    private final FileRecordRepository fileRecordRepository;

    public FileService(FileStoragePort fileStoragePort, FileRecordRepository fileRecordRepository) {
        this.fileStoragePort = fileStoragePort;
        this.fileRecordRepository = fileRecordRepository;
    }

    @Transactional
    public FileRecord upload(MultipartFile file) {
        FileUploadResultDto uploadResult = fileStoragePort.upload(file);
        FileRecord fileRecord = new FileRecord();
        fileRecord.setId(uploadResult.id());
        fileRecord.setOriginalFilename(uploadResult.originalFilename());
        fileRecord.setStorageKey(uploadResult.storageKey());
        fileRecord.setContentType(uploadResult.contentType());
        fileRecord.setSizeBytes(uploadResult.sizeBytes());
        fileRecord.setUploadedBy("system");
        fileRecord.setUploadedAt(Instant.now());
        return fileRecordRepository.save(fileRecord);
    }

    public FileRecord getFile(UUID fileId) {
        return fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("FILE_NOT_FOUND", "File not found"));
    }

    public Resource download(UUID fileId) {
        return fileStoragePort.download(getFile(fileId).getStorageKey());
    }
}
