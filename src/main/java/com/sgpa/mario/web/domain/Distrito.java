package com.sgpa.mario.web.domain;

import java.util.*;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "distrito")
public class Distrito extends AbstractEntity<Long> {

	  private String nome;

	    @ManyToOne
	    @JoinColumn(name = "Provincia_idProvincia")
	    private Provincia provincia;

	    @OneToMany(mappedBy = "distrito")
	    private List<Bairro> bairros;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Provincia getProvincia() {
		return provincia;
	}

	public void setProvincia(Provincia provincia) {
		this.provincia = provincia;
	}

	public List<Bairro> getBairros() {
		return bairros;
	}

	public void setBairros(List<Bairro> bairros) {
		this.bairros = bairros;
	}

    // getters e setters
    
}