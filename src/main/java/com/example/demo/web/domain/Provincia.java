package com.example.demo.web.domain;

import java.util.*;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "provincia")
public class Provincia extends AbstractEntity<Long> {

	 private String nome;

	    @OneToMany(mappedBy = "provincia")
	    private List<Distrito> distritos;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public List<Distrito> getDistritos() {
		return distritos;
	}

	public void setDistritos(List<Distrito> distritos) {
		this.distritos = distritos;
	}

    // getters e setters
    
}
