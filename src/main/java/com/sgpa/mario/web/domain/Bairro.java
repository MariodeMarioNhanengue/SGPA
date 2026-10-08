package com.sgpa.mario.web.domain;

import java.util.*;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "bairro")
public class Bairro extends AbstractEntity<Long> {

	private String nome;
	
	@ManyToOne
	@JoinColumn(name = "distrito_id")
	private Distrito distrito;
	
    @OneToMany(mappedBy = "bairro")
    private List<Oferta> ofertas;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Distrito getDistrito() {
		return distrito;
	}

	public void setDistrito(Distrito distrito) {
		this.distrito = distrito;
	}

	public List<Oferta> getOfertas() {
		return ofertas;
	}

	public void setOfertas(List<Oferta> ofertas) {
		this.ofertas = ofertas;
	}
}
