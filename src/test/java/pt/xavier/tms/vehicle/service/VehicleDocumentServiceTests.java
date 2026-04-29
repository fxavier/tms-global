package pt.xavier.tms.vehicle.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.shared.enums.VehicleDocumentType;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.vehicle.dto.VehicleDocumentDto;
import pt.xavier.tms.vehicle.repository.VehicleDocumentRepository;

@ExtendWith(MockitoExtension.class)
class VehicleDocumentServiceTests {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    @InjectMocks
    private VehicleDocumentService vehicleDocumentService;

    @Test
    void addDocumentRejectsFilesLargerThanTenMegabytes() {
        VehicleDocumentDto dto = new VehicleDocumentDto(
                VehicleDocumentType.SEGURO,
                "SEG-001",
                null,
                null,
                null,
                null,
                null,
                10L * 1024L * 1024L + 1L
        );

        assertThatThrownBy(() -> vehicleDocumentService.addDocument(java.util.UUID.randomUUID(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vehicle document file cannot exceed 10 MB");
    }
}
