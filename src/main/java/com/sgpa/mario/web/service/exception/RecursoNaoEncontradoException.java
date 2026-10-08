package com.example.demo.web.service.exception;

/**
 * Lançada pelos services quando uma entidade pedida não existe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
