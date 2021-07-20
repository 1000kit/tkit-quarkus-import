package org.tkit.quarkus.importer;

public class MasterDataImportRule {

    public String sourceKey;
    public String sourceFilePath;
    public ImportDataConfig.DataOperation dataOperation;
    public Class<?> classType;

    public MasterDataImportRule(String sourceKey, Class<?> classType) {
        this.sourceKey = sourceKey;
        this.classType = classType;
    }
}
