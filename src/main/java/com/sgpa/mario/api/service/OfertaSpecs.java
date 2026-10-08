package com.sgpa.mario.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.sgpa.mario.web.domain.*;
import com.sgpa.mario.web.domain.Oferta.EstadoOferta;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

final class OfertaSpecs {

    private OfertaSpecs() {}

    static Specification<Oferta> filtro(OfertaFiltro f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            EstadoOferta estado = f.estado() != null ? f.estado() : EstadoOferta.ATIVO;
            ps.add(cb.equal(root.get("estado"), estado));
            if (estado == EstadoOferta.ATIVO) {
                // esconde ofertas cuja validade já passou
                ps.add(cb.or(
                    cb.isNull(root.get("dataValidade")),
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("dataValidade"), LocalDateTime.now())));
            }

            if (f.nome() != null && !f.nome().isBlank()) {
                ps.add(cb.like(cb.lower(root.<String>get("nomeProduto")),
                    "%" + f.nome().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (f.categoria() != null && !f.categoria().isBlank()) {
                ps.add(cb.equal(cb.lower(root.<String>get("categoria")), f.categoria().trim().toLowerCase(Locale.ROOT)));
            }
            if (f.produtorId() != null) {
                ps.add(cb.equal(root.get("produtor").get("id"), f.produtorId()));
            }
            if (f.precoMin() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<java.math.BigDecimal>get("preco"), f.precoMin()));
            }
            if (f.precoMax() != null) {
                ps.add(cb.lessThanOrEqualTo(root.<java.math.BigDecimal>get("preco"), f.precoMax()));
            }
            if (f.dataInicio() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("dataPublicacao"), f.dataInicio().atStartOfDay()));
            }
            if (f.dataFim() != null) {
                ps.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("dataPublicacao"), f.dataFim().atTime(23, 59, 59)));
            }

            if (f.bairroId() != null || f.distritoId() != null || f.provinciaId() != null) {
                Join<Oferta, Bairro> bairro = root.join("bairro", JoinType.INNER);
                if (f.bairroId() != null) {
                    ps.add(cb.equal(bairro.get("id"), f.bairroId()));
                }
                if (f.distritoId() != null || f.provinciaId() != null) {
                    Join<Bairro, Distrito> distrito = bairro.join("distrito", JoinType.INNER);
                    if (f.distritoId() != null) {
                        ps.add(cb.equal(distrito.get("id"), f.distritoId()));
                    }
                    if (f.provinciaId() != null) {
                        ps.add(cb.equal(distrito.join("provincia", JoinType.INNER).get("id"), f.provinciaId()));
                    }
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
