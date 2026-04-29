package pt.xavier.tms.vehicle.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.vehicle.dto.VehicleCreateDto;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.service.VehicleService;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTests {

    @Mock
    private VehicleService vehicleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VehicleController(vehicleService)).build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void createVehicleReturnsCreatedEnvelope() throws Exception {
        Vehicle vehicle = vehicle();
        when(vehicleService.createVehicle(org.mockito.ArgumentMatchers.any(VehicleCreateDto.class))).thenReturn(vehicle);

        VehicleCreateDto request = new VehicleCreateDto(
                "AA-11-BB",
                "Mercedes",
                "Actros",
                "TRUCK",
                12000,
                "Lisboa",
                LocalDate.of(2025, 1, 1),
                VehicleStatus.DISPONIVEL,
                null,
                "Ready"
        );

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/vehicles/" + vehicle.getId()))
                .andExpect(jsonPath("$.data.id").value(vehicle.getId().toString()))
                .andExpect(jsonPath("$.data.plate").value("AA-11-BB"));
    }

    @Test
    void listVehiclesClampsPageSizeBetweenTenAndOneHundred() throws Exception {
        when(vehicleService.listVehicles(eq(VehicleStatus.DISPONIVEL), eq("Lisboa"), eq(PageRequest.of(0, 100))))
                .thenReturn(new PageImpl<>(java.util.List.of(vehicle()), PageRequest.of(0, 100), 1));

        mockMvc.perform(get("/api/v1/vehicles")
                        .param("status", "DISPONIVEL")
                        .param("location", "Lisboa")
                        .param("page", "-1")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.content[0].plate").value("AA-11-BB"));

        verify(vehicleService).listVehicles(VehicleStatus.DISPONIVEL, "Lisboa", PageRequest.of(0, 100));
    }

    private static Vehicle vehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setPlate("AA-11-BB");
        vehicle.setBrand("Mercedes");
        vehicle.setModel("Actros");
        vehicle.setVehicleType("TRUCK");
        vehicle.setCapacity(12000);
        vehicle.setActivityLocation("Lisboa");
        vehicle.setActivityStartDate(LocalDate.of(2025, 1, 1));
        vehicle.setStatus(VehicleStatus.DISPONIVEL);
        vehicle.setNotes("Ready");
        return vehicle;
    }
}
