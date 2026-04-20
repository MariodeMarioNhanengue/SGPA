package com.example.demo.web.repository;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.Oferta;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {
	
	

    // 🔝 Últimas ofertas
    List<Oferta> findTop3ByOrderByDataPublicacaoDesc();

    // 📊 Contar ofertas ativas
    @Query("SELECT COUNT(o) FROM Oferta o WHERE o.estado = com.example.demo.web.domain.Oferta$EstadoOferta.ATIVO")
    long contarOfertasAtivas();

    // 📅 Ofertas válidas hoje
    @Query("SELECT o FROM Oferta o WHERE o.dataValidade > CURRENT_TIMESTAMP")
    List<Oferta> ofertasValidas();

    // ⏳ Ofertas expiradas
    @Query("SELECT o FROM Oferta o WHERE o.dataValidade < CURRENT_TIMESTAMP")
    List<Oferta> ofertasExpiradas();

    // 📉 Ofertas com pouca quantidade
    @Query("SELECT o FROM Oferta o WHERE o.quantidadeRestante < :qtd")
    List<Oferta> ofertasComPoucoStock(@Param("qtd") BigDecimal qtd);

    // 👤 Ofertas de um produtor
    @Query("SELECT o FROM Oferta o WHERE o.produtor.id = :produtorId")
    List<Oferta> ofertasPorProdutor(Long produtorId);
}