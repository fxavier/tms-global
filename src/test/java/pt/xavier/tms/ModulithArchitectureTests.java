package pt.xavier.tms;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTests {

    @Test
    void verifiesModulithBoundaries() {
        ApplicationModules.of(TmsApplication.class).verify();
    }
}
