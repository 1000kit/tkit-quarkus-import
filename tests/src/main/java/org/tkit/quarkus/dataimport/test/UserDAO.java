package org.tkit.quarkus.dataimport.test;

import org.tkit.quarkus.jpa.daos.AbstractDAO;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserDAO extends AbstractDAO<User> {

}
