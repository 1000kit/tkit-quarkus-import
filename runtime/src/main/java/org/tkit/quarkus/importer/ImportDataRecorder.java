package org.tkit.quarkus.importer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;
import org.tkit.quarkus.importer.dao.ImportDataHistory;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
@ApplicationScoped
public class ImportDataRecorder {

    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    private static final Logger log = Logger.getLogger(ImportDataRecorder.class);

    static List<MasterDataImportRule> importRules = new ArrayList<>();

    public void loadClasses(Map<String, String> classes) throws ClassNotFoundException {
        for (String key : classes.keySet()) {
            Class<?> classType = Class.forName(classes.get(key), false, Thread.currentThread().getContextClassLoader());
            importRules.add(new MasterDataImportRule(key, classType));
        }
    }

    public void executeMasterDataImportRules(BeanContainer container, ApplicationConfig config, ImportDataConfig importConfig) throws ClassNotFoundException {
        if (importRules == null || importRules.isEmpty()) {
            return;
        }
        Arc.container().requestContext().activate();
        try {
            TransactionManager tm = Arc.container().instance(TransactionManager.class).get();
            ObjectMapper mapper = Arc.container().instance(ObjectMapper.class).get();
            EntityManager em = getEntityManager(null);

            for (MasterDataImportRule masterDataImportRule : importRules) {
                // First try to find file
                ImportDataConfig.DataImportConfiguration cof = importConfig.parameters.get(masterDataImportRule.sourceKey);
                if (cof == null) {
                    log.error("No configuration set for key='" + masterDataImportRule.sourceKey + "', skipping!");
                    continue;
                }
                masterDataImportRule.sourceFilePath = cof.file;
                if (masterDataImportRule.sourceFilePath == null) {
                    log.error("No file configuration set for key='" + masterDataImportRule.sourceKey + "', skipping!");
                    continue;
                }
                masterDataImportRule.dataOperation = cof.operation;

                try {
                    performDataImport(em, tm, mapper, masterDataImportRule);
                } catch (Exception e) {
                    log.error("Error occurred! " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            Arc.container().requestContext().deactivate();
        }
    }
    
    @Transactional
    public void performDataImport(EntityManager em, TransactionManager tm, ObjectMapper mapper, MasterDataImportRule rule) throws Exception {
        String md5 = createChecksum(rule.sourceFilePath);
        ImportDataHistory history = em.find(ImportDataHistory.class, rule.sourceKey);
        if (history != null) {
            if (md5.equals(history.getMd5())) {
                log.info("Import data " + rule.sourceKey + " from file " + rule.sourceFilePath + " is up to date!");
                return;
            }
            history.setFile(rule.sourceFilePath);
            history.setMd5(md5);
        }

        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, rule.classType);
        List<AbstractTraceableEntity<?>> sourceEntries = mapper.readValue(new File(rule.sourceFilePath), type);

        if (rule.dataOperation == ImportDataConfig.DataOperation.IGNORE_NEW) {
            return;
        }


        if (rule.dataOperation == ImportDataConfig.DataOperation.REPLACE) {
            tm.begin();
            for (AbstractTraceableEntity<?> obj : sourceEntries) {
                    em.persist(obj);
            }
            if (history != null) {
                em.merge(history);
            } else {
                ImportDataHistory h = new ImportDataHistory();
                h.setMd5(md5);
                h.setFile(rule.sourceFilePath);
                h.setKey(rule.sourceKey);
                em.persist(h);
            }
            tm.commit();
        }
    }

    private static String createChecksum(String file) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(file));
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] hash = md5.digest(data);
        return DatatypeConverter.printHexBinary(hash).toUpperCase();
    }


    public EntityManager getEntityManager(String persistentUnitName) {
        ArcContainer arcContainer = Arc.container();
        if (persistentUnitName == null) {
            InstanceHandle<EntityManager> emHandle = arcContainer.instance(EntityManager.class);
            if (emHandle.isAvailable()) {
                return emHandle.get();
            }
            if (!arcContainer.instance(AgroalDataSource.class).isAvailable()) {
                throw new IllegalStateException(
                        "The default datasource has not been properly configured. See https://quarkus.io/guides/datasource#jdbc-datasource for information on how to do that.");
            }
            throw new IllegalStateException(
                    "No entities were found. Did you forget to annotate your Panache Entity classes with '@Entity'?");
        }

        InstanceHandle<EntityManager> emHandle = arcContainer.instance(EntityManager.class,
                new PersistenceUnit.PersistenceUnitLiteral(persistentUnitName));
        if (emHandle.isAvailable()) {
            return emHandle.get();
        }
        if (!arcContainer.instance(AgroalDataSource.class, new DataSource.DataSourceLiteral(persistentUnitName))
                .isAvailable()) {
            throw new IllegalStateException("The named datasource '" + persistentUnitName
                    + "' has not been properly configured. See https://quarkus.io/guides/datasource#multiple-datasources for information on how to do that.");
        }
        throw new IllegalStateException("No entities were attached to persistence unit '" + persistentUnitName
                + "'. Did you forget to annotate your Panache Entity classes with '@Entity' or improperly configure the 'quarkus.hibernate-orm.\" "
                + persistentUnitName + "\".packages' property?");
    }
}
