package com.example.demo.web.service.exception;

/**
 * Lançada pelos services quando uma regra de negócio é violada
 * (ex: e-mail duplicado, quantidade indisponível, estado inválido).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
