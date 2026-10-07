package com.example.demo.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.demo.web.domain.Oferta.EstadoOferta;

/** Filtros opcionais da pesquisa de ofertas. */
public record OfertaFiltro(String nome, String categoria, Long provinciaId, Long distritoId, Long bairroId,
                           BigDecimal precoMin, BigDecimal precoMax, Long produtorId, EstadoOferta estado,
                           LocalDate dataInicio, LocalDate dataFim) {}
