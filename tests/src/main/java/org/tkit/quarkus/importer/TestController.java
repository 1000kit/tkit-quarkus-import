package org.tkit.quarkus.importer;

import javax.persistence.Table;

public class TestController {
    
    @ImportMasterData(key = "param1")
    @Table(name = "param_table")
    class TestData {

    }

}
