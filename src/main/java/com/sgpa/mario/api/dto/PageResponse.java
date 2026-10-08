package com.example.demo.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/** Resposta paginada. {@code pagina} começa em 0. */
public record PageResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    public static <T> PageResponse<T> de(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /** Pagina uma lista já carregada em memória. */
    public static <T> PageResponse<T> de(List<T> todos, int pagina, int tamanho) {
        int total = todos.size();
        int de = (int) Math.min((long) pagina * tamanho, total);
        int ate = Math.min(de + tamanho, total);
        int paginas = tamanho == 0 ? 0 : (int) Math.ceil(total / (double) tamanho);
        return new PageResponse<>(todos.subList(de, ate), pagina, tamanho, total, paginas);
    }
}
