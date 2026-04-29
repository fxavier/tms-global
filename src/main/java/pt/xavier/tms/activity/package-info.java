@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "vehicle::entity",
                "driver::entity",
                "integration::port",
                "integration::dto",
                "audit::annotation"
        }
)
package pt.xavier.tms.activity;
