package org.tkit.quarkus.dataimport;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

@Recorder
@ApplicationScoped
public class DataImportRecorder {

    private static final Logger LOGGER = Logger.getLogger(DataImportRecorder.class);

    public void createContext(DataImportRuntimeConfig config, Map<String, String> beans) {
        if (!config.enabled) {
            LOGGER.info("Data import is disabled.");
            return;
        }
        ArcContainer container = Arc.container();
        if (config.configurations != null) {

            container.requestContext().activate();
            try {
                DataImportInvoker invoker = container.instance(DataImportInvoker.class).get();

                for (Map.Entry<String, DataImportRuntimeConfig.DataImportConfiguration> item : config.configurations.entrySet()) {
                    String beanKey = item.getKey();
                    DataImportRuntimeConfig.DataImportConfiguration itemConfig = item.getValue();
                    if (itemConfig.bean != null && !itemConfig.bean.isBlank()) {
                        beanKey = itemConfig.bean;
                    }

                    String beanRef = beans.get(beanKey);

                    // check if the configuration for the key is defined
                    if (beanRef == null) {
                        LOGGER.warn("Missing bean implementation for data import key: " + item.getKey() + " bean: " + beanKey);
                        return;
                    }

                    // process data import item
                    try {
                        invoker.processItem(item.getKey(), beanRef, itemConfig);
                    } catch (Exception e) {
                        if (itemConfig.stopAtError) {
                            LOGGER.error("Error data import. Bean : " + item.getValue() + " key: " + item.getKey() + " error: " + e.getMessage(), e);
                            throw new RuntimeException("Error data import for key: " + item.getKey(), e);
                        }
                        LOGGER.warn("Error data import. Bean: " + item.getValue() + " key: " + item.getKey() + " error: " + e.getMessage(), e);
                    }
                }
            } finally {
              container.requestContext().deactivate();
            }
        }
    }

}
