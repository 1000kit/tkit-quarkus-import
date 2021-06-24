package org.tkit.quarkus.importer;

import org.hibernate.annotations.Entity;

public class TestController {
    
    @ImportMasterData(key = "param1")
    class TestData {

    }

}
