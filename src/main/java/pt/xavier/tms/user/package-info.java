@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "audit::event",
                "security::util"
        }
)
package pt.xavier.tms.user;
