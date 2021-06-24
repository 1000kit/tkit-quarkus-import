package org.tkit.quarkus.importer;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "import-config", phase = ConfigPhase.BOOTSTRAP)
public class ImportConfig {

    /**
     * If set to true, the application will attempt to look up the configuration from Consul
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled;

}
