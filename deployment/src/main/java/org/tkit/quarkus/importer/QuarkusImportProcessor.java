package org.tkit.quarkus.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Table;

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
    public void configSources(ApplicationConfig appConfig, ImportRecorder recorder) {
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void configure(CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, ImportRecorder recorder, ApplicationConfig appConfig)
            throws MissingAnnotationException {
        BeanContainer container = beanContainer.getValue();

        DotName tableAnnotation = DotName.createSimple(Table.class.getName());

        Collection<AnnotationInstance> annotations = index.getIndex().getAnnotations(MASTER_DATA_ANNOTATION);
        List<MasterDataImportRule> rules = new ArrayList<>();
        for (AnnotationInstance ann : annotations) {
            AnnotationTarget target = ann.target();
            List<AnnotationInstance> tableAnnotationInstances = target.asClass().annotations().get(tableAnnotation);
            if (tableAnnotationInstances == null || tableAnnotationInstances.isEmpty()) {
                throw new MissingAnnotationException(
                        "Error: ImportMasterData-Annotation requires a Table-Annotation to be present!");
            }
            String sourceKey = ann.value("key").asString();
            String targetTable =  tableAnnotationInstances.get(0).value("name").asString();            
            log.info("Found MasterDataImportRule! Will try to insert data from key=" + sourceKey + " into table="
                    + targetTable);
            rules.add(new MasterDataImportRule(sourceKey, targetTable));
        }
        recorder.markMasterDataImportRules(container, rules, appConfig);
    }
}
