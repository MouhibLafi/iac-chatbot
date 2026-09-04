package com.company.iacchatbot.exception;

/**
 * Exception levée lorsqu'un utilisateur dépasse ses quotas de ressources (UC-13)
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
