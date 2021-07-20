package org.tkit.quarkus.importer.dao;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity;
import org.tkit.quarkus.jpa.models.TraceableEntity;


@Getter
@Setter
@Entity
@Table(name = "IMPORT_DATA_HISTORY")
public class ImportDataHistory {

    @Id
    private String key;

    private String file;

    private String md5;
}
