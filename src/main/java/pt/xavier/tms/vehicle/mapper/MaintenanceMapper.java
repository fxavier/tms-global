package pt.xavier.tms.vehicle.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import pt.xavier.tms.vehicle.dto.MaintenanceRecordDto;
import pt.xavier.tms.vehicle.dto.MaintenanceResponseDto;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    MaintenanceResponseDto toResponseDto(MaintenanceRecord record);

    MaintenanceRecordDto toDto(MaintenanceRecord record);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    MaintenanceRecord toEntity(MaintenanceRecordDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    void updateEntity(MaintenanceRecordDto dto, @MappingTarget MaintenanceRecord record);
}
