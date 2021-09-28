package org.tkit.quarkus.dataimport.test;

import lombok.Getter;
import lombok.Setter;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@ApplicationScoped
@Table(name = "USER_TABLE")
public class User extends TraceableEntity{

    String name;

}
