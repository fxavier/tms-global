package pt.xavier.tms.vehicle.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import pt.xavier.tms.vehicle.dto.VehicleDocumentDto;
import pt.xavier.tms.vehicle.dto.VehicleDocumentResponseDto;
import pt.xavier.tms.vehicle.entity.VehicleDocument;

@Mapper(componentModel = "spring")
public interface VehicleDocumentMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "fileId", source = "file.id")
    VehicleDocumentResponseDto toResponseDto(VehicleDocument document);

    @Mapping(target = "fileSizeBytes", expression = "java(document.getFile() == null ? null : document.getFile().getSizeBytes())")
    VehicleDocumentDto toDto(VehicleDocument document);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    VehicleDocument toEntity(VehicleDocumentDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    void updateEntity(VehicleDocumentDto dto, @MappingTarget VehicleDocument document);
}
