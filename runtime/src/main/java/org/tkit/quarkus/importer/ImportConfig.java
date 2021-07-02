package org.tkit.quarkus.importer;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
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


    /**
     * All configured Data Import Configurations 
     */
    @ConfigItem(name = "parameters")
    Map<String, DataImportConfiguration> parameters;

    /**
     * A DataImportConfiguration
     */
    @ConfigGroup
    public static class DataImportConfiguration {

        /**
         * The path to the data source file
         */
        @ConfigItem
        String file;

        /**
         * The operation to perform when the md5-hash of the file differs from the last imported one
         */
        @ConfigItem
        String operation;

    }
}