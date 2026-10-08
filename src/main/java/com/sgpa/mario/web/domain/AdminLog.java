package com.sgpa.mario.web.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@SuppressWarnings("serial")
@Entity
@Table(name = "admin_log")
public class AdminLog extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    /** Tipo de acção: CRIAR_PRODUTOR, REMOVER_COMPRADOR, etc. */
    @Column(name = "tipo_acao", nullable = false, length = 60)
    private String tipoAcao;

    /** Descrição legível da acção realizada */
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    /** Nome da pessoa afectada pela acção (ex: "João Silva") */
    @Column(name = "nome_afectado", length = 300)
    private String nomeAfectado;

    /** Tipo da pessoa afectada: ADMIN, PRODUTOR, COMPRADOR, ou null para acções próprias */
    @Column(name = "tipo_afectado", length = 20)
    private String tipoAfectado;

    /** Data/hora da acção */
    @Column(name = "data_acao", nullable = false)
    private LocalDateTime dataAcao;

    @PrePersist
    public void prePersist() {
        if (dataAcao == null) dataAcao = LocalDateTime.now();
    }

    // ── Getters / Setters ────────────────────────────────────
    public Admin getAdmin()                      { return admin; }
    public void  setAdmin(Admin admin)           { this.admin = admin; }

    public String getTipoAcao()                  { return tipoAcao; }
    public void   setTipoAcao(String tipoAcao)   { this.tipoAcao = tipoAcao; }

    public String getDescricao()                 { return descricao; }
    public void   setDescricao(String descricao) { this.descricao = descricao; }

    public String getNomeAfectado()                      { return nomeAfectado; }
    public void   setNomeAfectado(String nomeAfectado)   { this.nomeAfectado = nomeAfectado; }

    public String getTipoAfectado()                      { return tipoAfectado; }
    public void   setTipoAfectado(String tipoAfectado)   { this.tipoAfectado = tipoAfectado; }

    public LocalDateTime getDataAcao()                   { return dataAcao; }
    public void          setDataAcao(LocalDateTime d)    { this.dataAcao = d; }
}
