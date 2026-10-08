package com.example.demo.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@SuppressWarnings("serial")
@Entity
@Table(name = "admin")
public class Admin extends AbstractEntity<Long> {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String apelido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String senha;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    /** true = SuperAdmin (pode criar/remover outros admins) */
    @Column(name = "super_admin", nullable = false)
    private boolean superAdmin = false;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    // ================= GETTERS E SETTERS =================

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public boolean isSuperAdmin() { return superAdmin; }
    public void setSuperAdmin(boolean superAdmin) { this.superAdmin = superAdmin; }

    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }

    public String getNomeCompleto() { return nome + " " + apelido; }
}
