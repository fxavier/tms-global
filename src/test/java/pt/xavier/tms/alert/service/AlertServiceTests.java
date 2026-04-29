package pt.xavier.tms.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.alert.entity.Alert;
import pt.xavier.tms.alert.repository.AlertConfigurationRepository;
import pt.xavier.tms.alert.repository.AlertRepository;
import pt.xavier.tms.driver.entity.DriverDocument;
import pt.xavier.tms.driver.repository.DriverDocumentRepository;
import pt.xavier.tms.shared.enums.AlertSeverity;
import pt.xavier.tms.shared.enums.AlertType;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.VehicleDocumentType;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.VehicleDocument;
import pt.xavier.tms.vehicle.repository.MaintenanceRepository;
import pt.xavier.tms.vehicle.repository.VehicleDocumentRepository;

@ExtendWith(MockitoExtension.class)
class AlertServiceTests {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertConfigurationRepository alertConfigRepository;
    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;
    @Mock
    private DriverDocumentRepository driverDocumentRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private AlertService alertService;

    @Test
    void checkDocumentExpiryCreatesWarningAlertForDocumentExpiringIn20Days() {
        VehicleDocument document = new VehicleDocument();
        document.setId(UUID.randomUUID());
        document.setDocumentType(VehicleDocumentType.INSPECAO);
        document.setExpiryDate(LocalDate.now().plusDays(20));

        when(alertConfigRepository.findByAlertTypeAndEntityType(any(), any())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findByExpiryDateBetweenAndStatusNot(any(), any(), any()))
                .thenReturn(List.of(document));
        when(vehicleDocumentRepository.findByExpiryDateBeforeAndStatusNot(any(), any())).thenReturn(List.of());
        when(driverDocumentRepository.findByExpiryDateBetweenAndStatusNot(any(), any(), any())).thenReturn(List.of());
        when(driverDocumentRepository.findByExpiryDateBeforeAndStatusNot(any(), any())).thenReturn(List.of());
        when(alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(any(), any())).thenReturn(false);

        alertService.checkDocumentExpiry();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.DOCUMENT_EXPIRING);
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.AVISO);
        assertThat(captor.getValue().getEntityId()).isEqualTo(document.getId());
    }

    @Test
    void checkDocumentExpiryCreatesCriticalAlertForDocumentExpiringIn5Days() {
        DriverDocument document = new DriverDocument();
        document.setId(UUID.randomUUID());
        document.setExpiryDate(LocalDate.now().plusDays(5));

        when(alertConfigRepository.findByAlertTypeAndEntityType(any(), any())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findByExpiryDateBetweenAndStatusNot(any(), any(), any())).thenReturn(List.of());
        when(vehicleDocumentRepository.findByExpiryDateBeforeAndStatusNot(any(), any())).thenReturn(List.of());
        when(driverDocumentRepository.findByExpiryDateBetweenAndStatusNot(any(), any(), any()))
                .thenReturn(List.of(document));
        when(driverDocumentRepository.findByExpiryDateBeforeAndStatusNot(any(), any())).thenReturn(List.of());
        when(alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(any(), any())).thenReturn(false);

        alertService.checkDocumentExpiry();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.DOCUMENT_EXPIRING);
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICO);
        assertThat(captor.getValue().getEntityId()).isEqualTo(document.getId());
    }

    @Test
    void checkDocumentExpiryMarksExpiredDocumentAsExpired() {
        VehicleDocument expiredDocument = new VehicleDocument();
        expiredDocument.setId(UUID.randomUUID());
        expiredDocument.setDocumentType(VehicleDocumentType.SEGURO);
        expiredDocument.setStatus(DocumentStatus.VALIDO);
        expiredDocument.setExpiryDate(LocalDate.now().minusDays(1));

        when(alertConfigRepository.findByAlertTypeAndEntityType(any(), any())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findByExpiryDateBetweenAndStatusNot(any(), any(), any())).thenReturn(List.of());
        when(vehicleDocumentRepository.findByExpiryDateBeforeAndStatusNot(any(), any()))
                .thenReturn(List.of(expiredDocument));
        when(driverDocumentRepository.findByExpiryDateBetweenAndStatusNot(any(), any(), any())).thenReturn(List.of());
        when(driverDocumentRepository.findByExpiryDateBeforeAndStatusNot(any(), any())).thenReturn(List.of());
        when(alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(any(), any())).thenReturn(false);

        alertService.checkDocumentExpiry();

        assertThat(expiredDocument.getStatus()).isEqualTo(DocumentStatus.EXPIRADO);
    }

    @Test
    void createAlertIfNotExistsDoesNotCreateDuplicateAlert() {
        UUID entityId = UUID.randomUUID();
        Alert existing = new Alert();
        existing.setSeverity(AlertSeverity.AVISO);

        when(alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(AlertType.DOCUMENT_EXPIRING, entityId))
                .thenReturn(true);
        when(alertRepository.findByAlertTypeAndEntityIdAndResolvedFalse(AlertType.DOCUMENT_EXPIRING, entityId))
                .thenReturn(Optional.of(existing));

        alertService.createAlertIfNotExists(
                AlertType.DOCUMENT_EXPIRING,
                "VEHICLE_DOCUMENT",
                entityId,
                AlertSeverity.AVISO,
                "title",
                "message"
        );

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void createAlertIfNotExistsEscalatesSeverityWhenExistingAlertHasLowerSeverity() {
        UUID entityId = UUID.randomUUID();
        Alert existing = new Alert();
        existing.setSeverity(AlertSeverity.AVISO);

        when(alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(AlertType.DOCUMENT_EXPIRING, entityId))
                .thenReturn(true);
        when(alertRepository.findByAlertTypeAndEntityIdAndResolvedFalse(AlertType.DOCUMENT_EXPIRING, entityId))
                .thenReturn(Optional.of(existing));

        alertService.createAlertIfNotExists(
                AlertType.DOCUMENT_EXPIRING,
                "VEHICLE_DOCUMENT",
                entityId,
                AlertSeverity.CRITICO,
                "title",
                "message"
        );

        assertThat(existing.getSeverity()).isEqualTo(AlertSeverity.CRITICO);
        verify(alertRepository).save(existing);
    }

    @Test
    void checkMaintenanceDueCreatesMaintenanceOverdueAlertWhenDateHasPassed() {
        UUID maintenanceId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate("00-AA-00");

        MaintenanceRecord record = new MaintenanceRecord();
        record.setId(maintenanceId);
        record.setVehicle(vehicle);
        record.setNextMaintenanceDate(LocalDate.now().minusDays(1));

        when(alertConfigRepository.findByAlertTypeAndEntityType(any(), any())).thenReturn(Optional.empty());
        when(maintenanceRepository.findByNextMaintenanceDateBetween(any(), any())).thenReturn(List.of());
        when(maintenanceRepository.findByNextMaintenanceDateBefore(any())).thenReturn(List.of(record));
        when(alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(any(), any())).thenReturn(false);

        alertService.checkMaintenanceDue();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.MAINTENANCE_OVERDUE);
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICO);
        assertThat(captor.getValue().getEntityId()).isEqualTo(maintenanceId);
    }
}
