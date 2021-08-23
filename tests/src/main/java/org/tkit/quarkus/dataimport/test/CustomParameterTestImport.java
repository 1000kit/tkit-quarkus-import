package org.tkit.quarkus.dataimport.test;

import org.tkit.quarkus.dataimport.DataImport;
import org.tkit.quarkus.dataimport.DataImportConfig;
import org.tkit.quarkus.dataimport.DataImportService;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

@DataImport("custom")
public class CustomParameterTestImport implements DataImportService {

    @Inject
    ParameterTestEntityDAO dao;

    @Override
    public void importData(DataImportConfig config) {
        System.out.println("File: " + config.getFile());
        System.out.println("MD5: " + config.getMD5());
        System.out.println("Metadata: " + config.getMetadata());
        System.out.println("Data: \n" + new String(config.getData(), StandardCharsets.UTF_8));
    }
}
