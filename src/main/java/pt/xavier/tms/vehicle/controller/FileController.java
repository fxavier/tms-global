package pt.xavier.tms.vehicle.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.vehicle.dto.FileResponseDto;
import pt.xavier.tms.vehicle.entity.FileRecord;
import pt.xavier.tms.vehicle.service.FileService;

@RestController
@RequestMapping("/api/v1/files")
@ConditionalOnProperty(name = "tms.vehicle.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','MOTORISTA')")
    public ResponseEntity<ApiResponse<FileResponseDto>> upload(@RequestParam("file") MultipartFile file) {
        FileResponseDto response = VehicleDtoMapper.toFileResponse(fileService.upload(file));
        return ResponseEntity.created(URI.create("/api/v1/files/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR','MOTORISTA')")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        FileRecord fileRecord = fileService.getFile(id);
        Resource resource = fileService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileRecord.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileRecord.getOriginalFilename())
                        .build()
                        .toString())
                .body(resource);
    }
}
