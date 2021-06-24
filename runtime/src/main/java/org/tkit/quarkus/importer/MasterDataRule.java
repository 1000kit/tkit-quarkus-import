package org.tkit.quarkus.importer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Table(name = "MASTER_DATA_TABLE")
@Entity
@Data
public class MasterDataRule {

    @Id
    private String key;
    private String pathToFile;
    private String md5;
}
