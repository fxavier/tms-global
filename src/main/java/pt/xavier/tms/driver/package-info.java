@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "vehicle",
                "hr::entity",
                "hr::repository",
                "audit::annotation",
                "integration"
        }
)
package pt.xavier.tms.driver;
