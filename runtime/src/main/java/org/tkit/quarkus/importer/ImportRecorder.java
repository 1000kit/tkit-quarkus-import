package org.tkit.quarkus.importer;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;


@Recorder
public class ImportRecorder {
    

    private static final Logger log = Logger.getLogger(ImportRecorder.class);


    public void annotationFinder(BeanContainer container) {
        log.info("Test Message");
    }
}
