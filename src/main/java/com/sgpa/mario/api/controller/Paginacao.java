package com.sgpa.mario.api.controller;

/** Limita os parâmetros de paginação a valores seguros. */
final class Paginacao {

    static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {}

    static int pagina(int pagina) {
        return Math.max(0, pagina);
    }

    static int tamanho(int tamanho) {
        return Math.min(Math.max(1, tamanho), TAMANHO_MAXIMO);
    }
}
