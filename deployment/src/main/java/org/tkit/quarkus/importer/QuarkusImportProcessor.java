package org.tkit.quarkus.importer;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.ApplicationConfig;

class QuarkusImportProcessor {

    private static final String FEATURE = "quarkus-import";

    private static final Logger log = Logger.getLogger(QuarkusImportProcessor.class);

    private static final DotName MASTER_DATA_ANNOTATION = DotName.createSimple(ImportMasterData.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep()
    public void test(CombinedIndexBuildItem index) {
        Collection<AnnotationInstance> annos = index.getIndex()
                .getAnnotations(DotName.createSimple(ImportMasterData.class.getName()));
        for (AnnotationInstance ann : annos) {
            log.info("Found Annotation: " + ann.toString());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void configSources(ImportConfig config, ApplicationConfig appConfig, ImportRecorder recorder) {
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void configure(CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, ImportRecorder recorder) {
        BeanContainer container = beanContainer.getValue();

        // DotName entityAnnotation = DotName.createSimple(Entity.class.getName());

        Collection<AnnotationInstance> annotations = index.getIndex().getAnnotations(MASTER_DATA_ANNOTATION);
        log.info("Found " + annotations.size() + " Annotations!");
        for (AnnotationInstance ann : annotations) {
            AnnotationTarget target = ann.target();
            // target.asClass().annotations().get(entityAnnotation);
            log.info("Found Annotation: " + ann.toString() + " using " + ann.value("key").asString());

        }
        recorder.annotationFinder(container);
    }
}
