package org.tkit.quarkus.importer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;
import org.tkit.quarkus.importer.MasterDataImportRule.DataOperation;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ImportRecorder {

    @Inject
    static EntityManager em;

    private static final Logger log = Logger.getLogger(ImportRecorder.class);

    static Map<String, String> importClasses;
    static List<MasterDataImportRule> importRules = new ArrayList<>();

    public void loadClasses(Map<String, String> classes) throws ClassNotFoundException {
        for (String key : classes.keySet()) {
            Class<?> classType = Class.forName(classes.get(key), false, Thread.currentThread().getContextClassLoader());
            importRules.add(new MasterDataImportRule(key, classType));
        }
    }

    public void executeMasterDataImportRules(BeanContainer container, ApplicationConfig config,
            ImportConfig importConfig) throws ClassNotFoundException {
        log.info("Received " + importRules.size() + " Import Rule(s)!");
        for (MasterDataImportRule masterDataImportRule : importRules) {
            // First try to find file
            String pathToFile = importConfig.parameters.get(masterDataImportRule.getSourceKey()).file;
            DataOperation dataOperation = DataOperation
                    .valueOf(importConfig.parameters.get(masterDataImportRule.getSourceKey()).operation);

            if (pathToFile == null) {
                log.error("No file configuration set for key='" + masterDataImportRule.getSourceKey() + "', skipping!");
                continue;
            }

            masterDataImportRule.setSourceFilePath(pathToFile);
            masterDataImportRule.setDataOperation(dataOperation);

            log.info("Using '" + pathToFile + "' as data source for '" + masterDataImportRule.getSourceKey() + "'");
            log.info("Target Class is " + masterDataImportRule.getClassType().getName());
            try {
                performDataImport(masterDataImportRule);
            } catch (Exception e) {
                log.error("Error occured! " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    public void performDataImport(MasterDataImportRule rule) throws Exception {
        log.info("+++++++ Performing MasterDataImport +++++++");
        log.info("Working Directory = " + System.getProperty("user.dir"));
        log.info("EntityManager: " + em);
        File sourceFile = new File(rule.getSourceFilePath());
        String md5 = MD5Checksum.getMD5Checksum(sourceFile);
        List<AbstractTraceableEntity<?>> sourceEntries = readSourceFile(rule.getClassType(), sourceFile);
        log.info("Found " + sourceEntries.size() + " Source Entries in Data-Source!");
        for (AbstractTraceableEntity<?> obj : sourceEntries) {
            log.info(" => Having id: " + ((AbstractTraceableEntity<?>) obj).getId().toString());
        }
        // Compare to stored md5, if not equal, perform data operation
        if (!md5.equals(getStoredMd5(rule.getSourceKey()))) {
            log.info("New MasterData! Performing data operation...");
            if (rule.getDataOperation() == DataOperation.IGNORE_NEW)
                return;
            if (rule.getDataOperation() == DataOperation.REPLACE) {
                // First remove old data
                for (AbstractTraceableEntity<?> obj : sourceEntries) {
                    try {
                        em.refresh(obj);
                    } catch (EntityNotFoundException notFound) {
                        // If not found => create
                        em.persist(obj);
                    }
                }
            }
        } else {
            log.info("MasterData is up to date!");
        }
    }

    private <T> List<AbstractTraceableEntity<?>> readSourceFile(Class<T> classType, File sourceFile)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, classType);
        return mapper.readValue(sourceFile, type);
    }

    private String getStoredMd5(String key) {
        // MasterDataRule rule = em.find(MasterDataRule.class, key);
        MasterDataRuleEntity rule = new MasterDataRuleEntity();
        return rule == null ? null : rule.getMd5();
    }
}
