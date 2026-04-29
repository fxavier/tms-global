package pt.xavier.tms.shared.exception;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsResourceNotFoundToNotFoundResponse() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Vehicle not found"));
    }

    @Test
    void mapsBusinessExceptionToUnprocessableEntityResponse() throws Exception {
        mockMvc.perform(get("/business-error"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.error.message").value("Plate already exists"));
    }

    @Test
    void mapsAllocationExceptionToUnprocessableEntityResponse() throws Exception {
        mockMvc.perform(get("/allocation-error"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ALLOCATION_BLOCKED"))
                .andExpect(jsonPath("$.error.message").value("Vehicle unavailable"));
    }

    @Test
    void mapsValidationErrorsToBadRequestResponse() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.name").value("must not be blank"));
    }

    @Test
    void mapsUnexpectedExceptionsToInternalServerErrorResponse() throws Exception {
        mockMvc.perform(get("/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.correlationId", notNullValue()));
    }

    @RestController
    private static class TestController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Vehicle not found");
        }

        @GetMapping("/business-error")
        void businessError() {
            throw new BusinessException("Plate already exists");
        }

        @GetMapping("/allocation-error")
        void allocationError() {
            throw new AllocationException("Vehicle unavailable");
        }

        @GetMapping("/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody TestRequest request) {
        }
    }

    private record TestRequest(@NotBlank String name) {
    }
}
