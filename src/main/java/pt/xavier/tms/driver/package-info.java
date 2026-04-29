@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "vehicle",
                "audit::annotation",
                "integration::port",
                "integration::dto"
        }
)
package pt.xavier.tms.driver;
