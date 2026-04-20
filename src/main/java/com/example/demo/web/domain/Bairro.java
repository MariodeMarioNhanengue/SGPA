package com.example.demo.web.domain;

import java.util.*;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "bairro")
public class Bairro extends AbstractEntity<Long> {

	private String nome;
	@ManyToOne
    @JoinColumn(name = "Distrito_idDistrito")
    private Distrito distrito;

    @OneToMany(mappedBy = "bairro")
    private List<Oferta> ofertas;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}



    // getters e setters
}
