package com.sgpa.mario.api.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgpa.mario.web.domain.Oferta;
import com.sgpa.mario.web.domain.Oferta.EstadoOferta;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    // =========================
    // 🔝 ÚLTIMAS OFERTAS
    // =========================
    List<Oferta> findTop3ByOrderByDataPublicacaoDesc();

    List<Oferta> findTop5ByOrderByDataPublicacaoDesc();

    // =========================
    // 📊 CONTAGEM
    // =========================
    long countByEstado(EstadoOferta estado);

    // ✅ Alternativa limpa ao @Query
    default long contarOfertasAtivas() {
        return countByEstado(EstadoOferta.ATIVO);
    }

    // =========================
    // 📅 VALIDADE
    // =========================
    @Query("SELECT o FROM Oferta o WHERE o.dataValidade IS NOT NULL AND o.dataValidade > CURRENT_TIMESTAMP")
    List<Oferta> ofertasValidas();

    @Query("SELECT o FROM Oferta o WHERE o.dataValidade IS NOT NULL AND o.dataValidade < CURRENT_TIMESTAMP")
    List<Oferta> ofertasExpiradas();

    // =========================
    // 📉 STOCK BAIXO
    // =========================
    @Query("SELECT o FROM Oferta o WHERE o.quantidadeRestante < :qtd")
    List<Oferta> ofertasComPoucoStock(@Param("qtd") BigDecimal qtd);

    // =========================
    // 👤 POR PRODUTOR (ESSENCIAL)
    // =========================
    @Query("SELECT o FROM Oferta o WHERE o.produtor.id = :produtorId")
    List<Oferta> ofertasPorProdutor(@Param("produtorId") Long produtorId);

    // =========================
    // 🔎 FILTROS ÚTEIS
    // =========================
    List<Oferta> findByEstado(EstadoOferta estado);

    List<Oferta> findByCategoria(String categoria);

    List<Oferta> findByNomeProdutoContainingIgnoreCase(String nome);

    // =========================
    // 📦 ORDENAÇÃO
    // =========================
    List<Oferta> findByOrderByPrecoAsc();

    List<Oferta> findByOrderByPrecoDesc();

    // =========================
    // 📅 FILTRO POR DATA DE PUBLICAÇÃO
    // =========================
    @Query("""
            SELECT o FROM Oferta o
            WHERE o.estado = 'ATIVO'
              AND (:dataInicio IS NULL OR o.dataPublicacao >= :dataInicio)
              AND (:dataFim    IS NULL OR o.dataPublicacao <= :dataFim)
            ORDER BY o.dataPublicacao DESC
            """)
    List<Oferta> findAtivasByDataPublicacao(
            @Param("dataInicio") java.time.LocalDateTime dataInicio,
            @Param("dataFim")    java.time.LocalDateTime dataFim);
}
// This line intentionally left blank
