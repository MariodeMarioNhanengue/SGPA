package com.example.demo.web.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@SuppressWarnings("serial")
@Entity
@Table(name = "produtor")
public class Produtor extends AbstractEntity<Long> {
 
    @Column(nullable = false, length = 150)
    private String nome;
 
    @Column(length = 20)
    private String telefone;
 
    @Column(unique = true)
    private String email;
 
    @Column(length = 100)
    private String senha;

    // Null = conta criada antes desta funcionalidade (não é bloqueada).
    // false = conta nova, aguarda confirmação. true = email confirmado.
    @Column(name = "email_verificado")
    private Boolean emailVerificado;
 
    @Column(length = 20, unique = true, nullable = true)
    private String nuit;
 
    @Column(nullable = false, length = 150)
    private String apelido;
 
    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    // RELACIONAMENTO COM OFERTAS
    @OneToMany(mappedBy = "produtor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Oferta> ofertas;
 
    // ================= GETTERS E SETTERS =================
 
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
 
    public String getApelido() {
        return apelido;
    }
 
    public void setApelido(String apelido) {
        this.apelido = apelido;
    }
 
    public List<Oferta> getOfertas() {
        return ofertas;
    }
 
    public void setOfertas(List<Oferta> ofertas) {
        this.ofertas = ofertas;
    }
 
    public String getSenha() {
        return senha;
    }
 
    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }

    // ================= MÉTODO EXTRA =================
 
    @Transient
    public String getNomeCompleto() {
        return nome + " " + apelido;
    }
}