package pt.xavier.tms.vehicle.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import pt.xavier.tms.vehicle.dto.VehicleCreateDto;
import pt.xavier.tms.vehicle.dto.VehicleResponseDto;
import pt.xavier.tms.vehicle.dto.VehicleUpdateDto;
import pt.xavier.tms.vehicle.entity.Vehicle;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleResponseDto toResponseDto(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "accessories", ignore = true)
    @Mapping(target = "maintenanceRecords", ignore = true)
    @Mapping(target = "checklistInspections", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    Vehicle toEntity(VehicleCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "accessories", ignore = true)
    @Mapping(target = "maintenanceRecords", ignore = true)
    @Mapping(target = "checklistInspections", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    void updateEntity(VehicleUpdateDto dto, @MappingTarget Vehicle vehicle);
}
