package com.sgpa.mario.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sgpa.mario.web.domain.Oferta.EstadoOferta;

/** Filtros opcionais da pesquisa de ofertas. */
public record OfertaFiltro(String nome, String categoria, Long provinciaId, Long distritoId, Long bairroId,
                           BigDecimal precoMin, BigDecimal precoMax, Long produtorId, EstadoOferta estado,
                           LocalDate dataInicio, LocalDate dataFim) {}
