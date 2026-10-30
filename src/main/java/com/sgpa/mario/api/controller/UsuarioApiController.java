package com.sgpa.mario.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sgpa.mario.api.config.UsuarioAtual;
import com.sgpa.mario.api.dto.UsuarioDtos.AlterarSenhaRequest;
import com.sgpa.mario.api.dto.UsuarioDtos.AtualizarPerfilRequest;
import com.sgpa.mario.api.dto.UsuarioDtos.RegistoCompradorRequest;
import com.sgpa.mario.api.dto.UsuarioDtos.RegistoProdutorRequest;
import com.sgpa.mario.api.dto.UsuarioDtos.UsuarioResponse;
import com.sgpa.mario.api.service.ApiUsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Utilizadores")
public class UsuarioApiController {

    private final ApiUsuarioService usuarioService;

    public UsuarioApiController(ApiUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ─── Registo (público) ───────────────────────────────────────────────

    @PostMapping(value = "/compradores", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Regista um Comprador (envia e-mail de confirmação)")
    public UsuarioResponse registarComprador(@Valid @RequestBody RegistoCompradorRequest req) {
        return usuarioService.registarComprador(req);
    }

    @PostMapping(value = "/produtores", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Regista um Produtor (envia e-mail de confirmação)")
    public UsuarioResponse registarProdutor(@Valid @RequestBody RegistoProdutorRequest req) {
        return usuarioService.registarProdutor(req);
    }

    // ─── Perfil do utilizador autenticado ────────────────────────────────

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Perfil do utilizador autenticado")
    public UsuarioResponse me() {
        return usuarioService.obter(UsuarioAtual.obter());
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualiza o perfil do utilizador autenticado")
    public UsuarioResponse atualizar(@Valid @RequestBody AtualizarPerfilRequest req) {
        return usuarioService.atualizar(UsuarioAtual.obter(), req);
    }

    @PutMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Envia a foto de perfil (multipart, campo 'foto': JPG/PNG/WEBP/GIF)")
    public UsuarioResponse foto(@RequestPart("foto") MultipartFile foto) {
        return usuarioService.atualizarFoto(UsuarioAtual.obter(), foto);
    }

    @PutMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Altera a senha (exige a senha actual)")
    public void alterarSenha(@Valid @RequestBody AlterarSenhaRequest req) {
        usuarioService.alterarSenha(UsuarioAtual.obter(), req);
    }
}
