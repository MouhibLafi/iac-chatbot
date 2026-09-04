package com.company.iacchatbot.exception;

/**
 * Exception levée lorsqu'un déploiement échoue (UC-06)
 */
public class DeploymentFailedException extends RuntimeException {

    public DeploymentFailedException(String message) {
        super(message);
    }

    public DeploymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
