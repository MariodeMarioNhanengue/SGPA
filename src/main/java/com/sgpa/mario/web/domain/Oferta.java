package com.sgpa.mario.web.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "oferta")
public class Oferta extends AbstractEntity<Long> {

    // ======================
    // DADOS DO PRODUTO
    // ======================
    @Column(nullable = false)
    private String categoria;

    @Column(name = "nome_produto", nullable = false)
    private String nomeProduto;

    @Column(nullable = false)
    private String unidade;

    // ======================
    // QUANTIDADES
    // ======================
    @Column(name = "quantidade_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantidadeTotal;

    @Column(name = "quantidade_restante", precision = 10, scale = 2)
    private BigDecimal quantidadeRestante;

    // ======================
    // PREÇO
    // ======================
    @Column(name = "preco", precision = 10, scale = 2, nullable = false)
    private BigDecimal preco;

    // ======================
    // DATAS
    // ======================
    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    @Column(name = "data_validade")
    private LocalDateTime dataValidade;

    // ======================
    // DESCRIÇÃO
    // ======================
    @Column(columnDefinition = "TEXT")
    private String descricao;

    // ======================
    // IMAGEM
    // ======================
    @Column(name = "imagem_path")
    private String imagemPath;

    // ======================
    // ESTADO
    // ======================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOferta estado;

    public enum EstadoOferta {
        ATIVO,
        INATIVO,
        EXPIRADO
    }

    // ======================
    // RELACIONAMENTOS
    // ======================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Produtor_idProdutor", nullable = false)
    private Produtor produtor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Bairro_idBairro", nullable = true)
    private Bairro bairro;

    // ======================
    // CONSTRUTOR
    // ======================
    public Oferta() {
        this.estado = EstadoOferta.ATIVO;
    }

    // ======================
    // MÉTODOS AUTOMÁTICOS
    // ======================
    @PrePersist
    public void prePersist() {

        // Data automática
        if (dataPublicacao == null) {
            dataPublicacao = LocalDateTime.now();
        }

        // Quantidade restante automática
        if (quantidadeRestante == null && quantidadeTotal != null) {
            quantidadeRestante = quantidadeTotal;
        }

        // Estado padrão
        if (estado == null) {
            estado = EstadoOferta.ATIVO;
        }
    }

    // ======================
    // GETTERS E SETTERS
    // ======================

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public BigDecimal getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public void setQuantidadeTotal(BigDecimal quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }

    public BigDecimal getQuantidadeRestante() {
        return quantidadeRestante;
    }

    public void setQuantidadeRestante(BigDecimal quantidadeRestante) {
        this.quantidadeRestante = quantidadeRestante;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public LocalDateTime getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(LocalDateTime dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

    public LocalDateTime getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDateTime dataValidade) {
        this.dataValidade = dataValidade;
    }


    public String getImagemPath() {
        return imagemPath;
    }

    public void setImagemPath(String imagemPath) {
        this.imagemPath = imagemPath;
    }
    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public EstadoOferta getEstado() {
        return estado;
    }

    public void setEstado(EstadoOferta estado) {
        this.estado = estado;
    }

    public Produtor getProdutor() {
        return produtor;
    }

    public void setProdutor(Produtor produtor) {
        this.produtor = produtor;
    }

    public Bairro getBairro() {
        return bairro;
    }

    public void setBairro(Bairro bairro) {
        this.bairro = bairro;
    }
}