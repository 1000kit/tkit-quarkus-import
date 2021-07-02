package org.tkit.quarkus.importer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;
import org.tkit.quarkus.importer.MasterDataImportRule.DataOperation;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
@ApplicationScoped
public class ImportRecorder {

    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    @Inject
    EntityManager em;

    private static final Logger log = Logger.getLogger(ImportRecorder.class);

    static Map<String, String> importClasses;
    static List<MasterDataImportRule> importRules = new ArrayList<>();

    public void loadClasses(Map<String, String> classes) throws ClassNotFoundException {
        for (String key : classes.keySet()) {
            Class<?> classType = Class.forName(classes.get(key), false, Thread.currentThread().getContextClassLoader());
            importRules.add(new MasterDataImportRule(key, classType));
        }
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

    public EntityManager getEntityManager() {
        return getEntityManager(null);
    }

    @ActivateRequestContext
    public void executeMasterDataImportRules(BeanContainer container, ApplicationConfig config,
            ImportConfig importConfig, HibernateOrmRecorder hibernateOrmRecorder) throws ClassNotFoundException {
        Arc.container().requestContext().activate();
        log.info("EM before: " + getEntityManager() + " context active " + Arc.container().requestContext().isActive());
        em = getEntityManager(); 
        // hibernateOrmRecorder.sessionSupplier("<default>").get().beginTransaction(); // Might help? 
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
    
    @Transactional
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

        
        
        em.getTransaction().begin();
        // Compare to stored md5, if not equal, perform data operation
        if (!md5.equals(getStoredMd5(rule.getSourceKey()))) {
            log.info("New MasterData! Performing data operation...");
            if (rule.getDataOperation() == DataOperation.IGNORE_NEW)
            {
                return;
            }                
            if (rule.getDataOperation() == DataOperation.REPLACE) {
                // First remove old data
                for (AbstractTraceableEntity<?> obj : sourceEntries) {
                    try {
                        em.refresh(obj);
                    } catch (EntityNotFoundException notFound) {
                        // If not found => create
                        em.persist(obj);
                    }
                    em.getTransaction().commit();
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
