package org.tkit.quarkus.dataimport.test;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Data;

@Getter
@Setter
@Entity
@ApplicationScoped
@Table(name = "param_table")
public class ParameterTestEntity extends TraceableEntity{

    String name;
    String key;
    String value;

}
