package com.example.demo.web.domain;

import java.util.*;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "produtor")
public class Produtor extends AbstractEntity<Long> {

	  private String nome;
	    private String telefone;
	    private String email;
	    private String nuit;

	    @OneToMany(mappedBy = "produtor")
	    private List<Oferta> ofertas;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNuit() {
		return nuit;
	}

	public void setNuit(String nuit) {
		this.nuit = nuit;
	}

	public List<Oferta> getOfertas() {
		return ofertas;
	}

	public void setOfertas(List<Oferta> ofertas) {
		this.ofertas = ofertas;
	}

    
}
