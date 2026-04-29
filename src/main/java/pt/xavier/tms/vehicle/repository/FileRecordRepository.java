package pt.xavier.tms.vehicle.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.vehicle.entity.FileRecord;

public interface FileRecordRepository extends JpaRepository<FileRecord, UUID> {
}
