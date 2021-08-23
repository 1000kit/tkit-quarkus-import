package org.tkit.quarkus.dataimport;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.Unremovable;
import org.jboss.logging.Logger;
import org.tkit.quarkus.dataimport.log.DataImportLog;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Objects;

import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;

@Unremovable
@ApplicationScoped
public class DataImportInvoker {

    private static final Logger LOGGER = Logger.getLogger(DataImportInvoker.class);

    @Inject
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRED)
    public void processItem(String key, String bean, DataImportRuntimeConfig.DataImportConfiguration config) throws Exception {
        // check if the data import fo the key is enabled
        if (!config.enabled) {
            LOGGER.info("Data import for key: " + key + " is disabled.");
            return;
        }

        // check if file is defined
        if (config.file == null || config.file.isBlank()) {
            LOGGER.warn("Data import file is not defined. Key: " + key);
            return;
        }

        // check if the file exists
        Path file = Paths.get(config.file);
        if (!Files.exists(file)) {
            LOGGER.info("Data import file does not exists. Key: " + key + " file: " + config.file);
            return;
        }

        // create parameter
        DataImportConfig param = new DataImportConfig();
        param.key = key;
        param.metadata = config.metadata;
        param.file = file;
        param.data = loadData(param.file);
        param.md5 = createChecksum(param.data);


        // find import entry and lock it
        DataImportLog log = em.find(DataImportLog.class, param.key, PESSIMISTIC_WRITE);
        if (log == null) {
            log = new DataImportLog();
            log.setId(param.key);
            log.setCreationDate(LocalDateTime.now());
            try {
                em.persist(log);
                em.flush();
            } catch (Exception ex) {
                // ignore
            }
            log = em.find(DataImportLog.class, param.key, PESSIMISTIC_WRITE);
        }

        // check the MD5
        if (log.getMd5() != null && Objects.equals(param.md5, log.getMd5())) {
            LOGGER.info("No changes found in the data import file. Key: " + key + " file: " + config.file);
            return;
        }

        // call data import service
        log.setModificationDate(LocalDateTime.now());
        log.setMd5(param.md5);
        log.setFile(param.file.toString());
        try {
            InjectableBean<DataImportService> bi = Arc.container().bean(bean);
            DataImportService service = Arc.container().instance(bi).get();
            service.importData(param);
        } catch (Exception ex) {
            log.setError(ex.getMessage());
            throw ex;
        } finally {
            em.merge(log);
            em.flush();
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    private void create(DataImportLog log) {
        em.persist(log);
        em.flush();
    }
    private static String createChecksum(byte[] data) throws RuntimeException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(data);
            return DatatypeConverter.printHexBinary(hash).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error create MD5 from the file content ", e);
        }
    }

    private static byte[] loadData(Path path) throws RuntimeException {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Error loading data from " + path, e);
        }
    }

}
