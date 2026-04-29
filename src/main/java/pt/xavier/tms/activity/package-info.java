@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "vehicle::entity",
                "vehicle::repository",
                "driver::entity",
                "driver::repository",
                "integration::port",
                "integration::dto",
                "audit::annotation"
        }
)
package pt.xavier.tms.activity;
