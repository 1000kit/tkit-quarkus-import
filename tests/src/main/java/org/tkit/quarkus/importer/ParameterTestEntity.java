package org.tkit.quarkus.importer;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Data;

@ImportMasterData(key = "param1")
@Table(name = "param_table")
@Entity
@Data
public class ParameterTestEntity extends TraceableEntity{

    String name;
    String key;
    String value;

}
