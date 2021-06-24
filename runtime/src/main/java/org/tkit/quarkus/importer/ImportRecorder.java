package org.tkit.quarkus.importer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.tkit.quarkus.importer.MasterDataImportRule.DataOperation;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ImportRecorder {

    @Inject
    private static EntityManager em;

    private static final Logger log = Logger.getLogger(ImportRecorder.class);
    private static String CONFIG_PREFIX = "quarkus.tkit.import.";

    public void markMasterDataImportRules(BeanContainer container, List<MasterDataImportRule> importRules,
            ApplicationConfig config) {
        log.info("Received " + importRules.size() + " Import Rules!");
        for (MasterDataImportRule masterDataImportRule : importRules) {
            // First try to find file
            String pathToFile = ConfigProvider.getConfig()
                    .getValue(CONFIG_PREFIX + masterDataImportRule.getSourceKey() + ".file", String.class);
            DataOperation dataOperation = DataOperation.valueOf(ConfigProvider.getConfig()
                    .getOptionalValue(CONFIG_PREFIX + masterDataImportRule.getSourceKey() + ".operation", String.class)
                    .orElse(DataOperation.REPLACE.name()));

            if (pathToFile == null) {
                log.error("No file configuration set for key='" + masterDataImportRule.getSourceKey() + "', skipping!");
                continue;
            }

            masterDataImportRule.setSourceFilePath(pathToFile);
            masterDataImportRule.setDataOperation(dataOperation);

            log.info("Using '" + pathToFile + "' as data source for '" + masterDataImportRule.getTargetTable() + "'");
            try {
                performDataImport(masterDataImportRule);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void performDataImport(MasterDataImportRule rule) throws Exception {
        log.info("+++++++ Performing MasterDataImport +++++++");
        File sourceFile = new File(rule.getSourceFilePath());
        String md5 = MD5Checksum.getMD5Checksum(sourceFile);
        // Compare to stored md5, if not equal, perform data operation
        if (!md5.equals(getStoredMd5(rule.getSourceKey()))) {
            log.info("New MasterData! Performing data operation...");
            if (rule.getDataOperation() == DataOperation.IGNORE_NEW)
                return;
            if (rule.getDataOperation() == DataOperation.REPLACE) {
                // First remove old data
                
            }
        } else {
            log.info("MasterData is up to date!");
        }
    }

    private List<ParameterEntry> readSourceFile(File sourceFile) throws JsonParseException, JsonMappingException, IOException{
        ObjectMapper mapper = new ObjectMapper();
        return Arrays.asList(mapper.readValue(sourceFile, ParameterEntry[].class));    
    }

    private String getStoredMd5(String key) {
        MasterDataRule rule = em.find(MasterDataRule.class, key);
        return rule == null ? null : rule.getMd5();
    }
}
