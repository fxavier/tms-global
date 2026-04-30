@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "vehicle::entity",
                "vehicle::repository",
                "driver::entity",
                "driver::repository",
                "hr::entity",
                "audit::annotation"
        }
)
package pt.xavier.tms.activity;
