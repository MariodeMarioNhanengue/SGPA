package com.example.demo.web.domain;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "produto")
public class Produto extends AbstractEntity<Long> {

	 private String nome;

	    @Column(columnDefinition = "TEXT")
	    private String descricao;

	    private String unidade;

	    @ManyToOne
	    @JoinColumn(name = "Categoria_idCategoria")
	    private Categoria categoria;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getUnidade() {
		return unidade;
	}

	public void setUnidade(String unidade) {
		this.unidade = unidade;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

    
}
