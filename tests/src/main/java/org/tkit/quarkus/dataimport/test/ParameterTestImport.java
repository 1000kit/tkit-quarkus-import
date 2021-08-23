package org.tkit.quarkus.dataimport.test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tkit.quarkus.dataimport.DataImport;
import org.tkit.quarkus.dataimport.DataImportConfig;
import org.tkit.quarkus.dataimport.DataImportService;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;

@DataImport("key1")
public class ParameterTestImport implements DataImportService {

    @Inject
    ParameterTestEntityDAO dao;

    @Inject
    ObjectMapper mapper;

    @Override
    public void importData(DataImportConfig config) {
        System.out.println("File: " + config.getFile());
        System.out.println("MD5: " + config.getMD5());
        System.out.println("Metadata: " + config.getMetadata());
        System.out.println("Data: \n" + new String(config.getData(), StandardCharsets.UTF_8));

        try {
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, ParameterTestEntity.class);
            List<ParameterTestEntity> entries = mapper.readValue(config.getData(), type);
            dao.create(entries);
        } catch (Exception ex) {
            throw new RuntimeException("Error import", ex);
        }
    }
}
