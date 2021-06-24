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
    private String targetTable;
    private String sourceFilePath;
    private DataOperation dataOperation;

    public MasterDataImportRule(String sourceKey, String targetTable) {
        this.sourceKey = sourceKey;
        this.targetTable = targetTable;
    }
}
