package pt.xavier.tms.documentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration",
        "tms.jpa.auditing.enabled=false",
        "tms.audit.enabled=false",
        "tms.vehicle.controllers.enabled=false",
        "tms.vehicle.services.enabled=false",
        "tms.driver.controllers.enabled=false",
        "tms.driver.services.enabled=false",
        "tms.activity.controllers.enabled=false",
        "tms.activity.services.enabled=false",
        "tms.alert.controllers.enabled=false",
        "tms.alert.services.enabled=false",
        "tms.hr.controllers.enabled=false",
        "tms.hr.services.enabled=false"
})
@AutoConfigureMockMvc
class SwaggerDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsArePublicAndIncludeBearerSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("TMS Global API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.oauth2.type").value("oauth2"))
                .andExpect(jsonPath("$.components.securitySchemes.oauth2.flows.password.tokenUrl")
                        .value("http://localhost:8081/realms/tms/protocol/openid-connect/token"));
    }

    @Test
    void apiDocsIncludeUserEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users'].get.responses['401'].description")
                        .value("Unauthorized - bearer token is missing or invalid"))
                .andExpect(jsonPath("$.paths['/api/v1/users'].get.responses['403'].description")
                        .value("Forbidden - authenticated user does not have the required role"))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get").exists());
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("swagger-ui-bundle.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("swagger-initializer.js")));
    }

    @Test
    void swaggerUiAssetsArePublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/css")));

        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("javascript")));

        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/v3/api-docs")));
    }
}
