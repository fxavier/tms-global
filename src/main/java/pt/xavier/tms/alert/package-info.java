@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "vehicle::repository",
                "vehicle::entity",
                "driver::repository",
                "driver::entity",
                "activity",
                "audit"
        }
)
package pt.xavier.tms.alert;
