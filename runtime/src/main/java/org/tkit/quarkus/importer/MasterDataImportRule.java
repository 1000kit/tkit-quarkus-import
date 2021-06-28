package org.tkit.quarkus.importer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MasterDataImportRule {

    enum DataOperation {
        REPLACE, IGNORE_NEW
    }

    private String sourceKey;
    private String sourceFilePath;
    private DataOperation dataOperation;
    private Class<?> classType;

    public MasterDataImportRule(String sourceKey, Class<?> classType) {
        this.sourceKey = sourceKey;
        this.classType = classType;
    }
}
