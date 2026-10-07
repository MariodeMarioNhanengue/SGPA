package com.example.demo.api.service.exception;

/**
 * Lançada pelos services quando o utilizador autenticado não tem
 * permissão para executar a acção pedida (regra de negócio de autorização).
 */
public class NaoAutorizadoException extends RuntimeException {

    public NaoAutorizadoException(String message) {
        super(message);
    }
}
