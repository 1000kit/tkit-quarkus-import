package org.tkit.quarkus.importer;

public class MissingAnnotationException extends RuntimeException{
    
    public MissingAnnotationException(String message){
        super(message);
    }
}
