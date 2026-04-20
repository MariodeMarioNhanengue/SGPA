package com.example.demo.web.domain;



import java.util.List;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "comprador")
public class Comprador extends AbstractEntity<Long> {

	private String nome;
    private String telefone;
    private String email;

    @OneToMany(mappedBy = "comprador")
    private List<Pedido> pedidos;

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

	public List<Pedido> getPedidos() {
		return pedidos;
	}

	public void setPedidos(List<Pedido> pedidos) {
		this.pedidos = pedidos;
	}

    
}
