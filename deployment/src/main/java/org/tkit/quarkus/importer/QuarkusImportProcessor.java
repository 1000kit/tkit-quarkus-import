package org.tkit.quarkus.importer;

import java.io.InvalidClassException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.tkit.quarkus.jpa.models.BusinessTraceableEntity;
import org.tkit.quarkus.jpa.models.TraceableEntity;

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
    private static final DotName ENTITY_ANNOTATION = DotName.createSimple(Entity.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void configure(CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, ImportRecorder recorder,
            ApplicationConfig appConfig, ImportConfig importConfig) throws Exception {
        BeanContainer container = beanContainer.getValue();
        recorder.executeMasterDataImportRules(container, appConfig, importConfig);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void test(CombinedIndexBuildItem index, ImportRecorder recorder) throws Exception {
        Collection<AnnotationInstance> annotations = index.getIndex().getAnnotations(MASTER_DATA_ANNOTATION);

        Map<String, String> classes = new HashMap<>();
        for (AnnotationInstance ann : annotations) {
            AnnotationTarget target = ann.target();
            List<AnnotationInstance> entityAnnotationInstances = target.asClass().annotations().get(ENTITY_ANNOTATION);

            if (entityAnnotationInstances == null || entityAnnotationInstances.isEmpty()) {
                throw new MissingAnnotationException(
                        "Error: ImportMasterData-Annotation requires a Entity-Annotation to be present!");
            }
            if (!target.asClass().superName().equals(DotName.createSimple(TraceableEntity.class.getName())) && !target
                    .asClass().superName().equals(DotName.createSimple(BusinessTraceableEntity.class.getName()))) {
                throw new InvalidClassException(
                        "Error: ImportMasterData-Entity needs to extend TraceableEntity/BusinessTraceableEntity!");
            }

            String sourceKey = ann.value("key").asString();
            log.info("Found MasterDataImportRule with key=" + sourceKey + " and Entity=" + target.toString());
            classes.put(sourceKey, target.toString());
        }
        recorder.loadClasses(classes);
    }
}
