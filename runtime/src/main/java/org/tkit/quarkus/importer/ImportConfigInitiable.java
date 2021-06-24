package org.tkit.quarkus.importer;

/**
 * For every service which need to be initialized using the import-config
 */
public interface ImportConfigInitiable {
    /**
     * Initialize this service 
     *
     * @param config the import-configuration
     */
    void init(ImportConfig config);
}
