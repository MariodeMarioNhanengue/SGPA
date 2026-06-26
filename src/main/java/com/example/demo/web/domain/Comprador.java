package com.example.demo.web.domain;


import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@SuppressWarnings("serial")
@Entity
@Table(name = "comprador")
public class Comprador extends AbstractEntity<Long> {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String apelido;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(length = 100)
    private String senha;

    @Column(length = 20, unique = true, nullable = true)
    private String nuit;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    // RELACIONAMENTO
    @OneToMany(mappedBy = "comprador", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pedido> pedidos;

      @ManyToOne
    @JoinColumn(name="id_bairro")
 private Bairro bairros;


    // ================= GETTERS E SETTERS =================
    public Bairro getBairros() {
		return bairros;
	}

	public void setBairros(Bairro bairros) {
		this.bairros = bairros;
	}

	public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
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

    public String getNuit() {
        return nuit;
    }

    public void setNuit(String nuit) {
        this.nuit = nuit;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }

    // ================= MÉTODO EXTRA (ÚTIL) =================
    
    @Transient
    public String getNomeCompleto() {
        return nome + " " + apelido;
    }
}