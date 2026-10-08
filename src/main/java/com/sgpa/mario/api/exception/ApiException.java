package com.example.demo.api.exception;

import org.springframework.http.HttpStatus;

/** Erro de negócio/HTTP da API com estado HTTP e código estável para o cliente. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
