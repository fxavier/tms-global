package pt.xavier.tms.vehicle.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import pt.xavier.tms.vehicle.dto.ChecklistInspectionDto;
import pt.xavier.tms.vehicle.dto.ChecklistTemplateResponseDto;
import pt.xavier.tms.vehicle.dto.VehicleAccessoryDto;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.entity.ChecklistTemplate;
import pt.xavier.tms.vehicle.entity.VehicleAccessory;

@Mapper(componentModel = "spring")
public interface ChecklistMapper {

    @Mapping(target = "templateId", source = "template.id")
    @Mapping(target = "items", source = "items", qualifiedByName = "toInspectionItems")
    ChecklistInspectionDto toInspectionDto(ChecklistInspection inspection);

    List<ChecklistInspectionDto> toInspectionDtos(List<ChecklistInspection> inspections);

    @Mapping(target = "active", source = "active")
    ChecklistTemplateResponseDto toTemplateResponseDto(ChecklistTemplate template);

    VehicleAccessoryDto toAccessoryDto(VehicleAccessory accessory);

    List<VehicleAccessoryDto> toAccessoryDtos(List<VehicleAccessory> accessories);

    @Named("toInspectionItems")
    default List<ChecklistInspectionDto.Item> toInspectionItems(List<pt.xavier.tms.vehicle.entity.ChecklistInspectionItem> items) {
        return items.stream()
                .map(item -> new ChecklistInspectionDto.Item(
                        item.getTemplateItem() == null ? null : item.getTemplateItem().getId(),
                        item.getItemName(),
                        item.isCritical(),
                        item.getStatus(),
                        item.getNotes()
                ))
                .toList();
    }
}
