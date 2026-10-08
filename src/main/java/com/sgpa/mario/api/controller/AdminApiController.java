package com.sgpa.mario.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sgpa.mario.api.config.UsuarioAtual;
import com.sgpa.mario.api.dto.AdminDtos.AdminLogResponse;
import com.sgpa.mario.api.dto.AdminDtos.AlterarEstadoContaRequest;
import com.sgpa.mario.api.dto.AdminDtos.CriarAdminRequest;
import com.sgpa.mario.api.dto.AdminDtos.CriarUtilizadorRequest;
import com.sgpa.mario.api.dto.AdminDtos.DetalheCompradorResponse;
import com.sgpa.mario.api.dto.AdminDtos.DetalheProdutorResponse;
import com.sgpa.mario.api.dto.AdminDtos.ResumoAdminResponse;
import com.sgpa.mario.api.dto.PageResponse;
import com.sgpa.mario.api.dto.PedidoDtos.PedidoResponse;
import com.sgpa.mario.api.dto.UsuarioDtos.UsuarioResponse;
import com.sgpa.mario.api.service.ApiAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Todos os endpoints exigem ROLE_ADMIN (cadeia de segurança); gestão de administradores exige SUPER_ADMIN. */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administração")
@SecurityRequirement(name = "bearerAuth")
public class AdminApiController {

    private final ApiAdminService adminService;

    public AdminApiController(ApiAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Estatísticas do painel")
    public ResumoAdminResponse resumo() {
        return adminService.resumo();
    }

    // ─── Produtores ──────────────────────────────────────────────────────

    @GetMapping("/produtores")
    @Operation(summary = "Lista/pesquisa produtores")
    public PageResponse<UsuarioResponse> listarProdutores(@RequestParam(required = false) String pesquisa,
                                                          @RequestParam(defaultValue = "0") int pagina,
                                                          @RequestParam(defaultValue = "20") int tamanho) {
        return adminService.listarProdutores(pesquisa, Paginacao.pagina(pagina), Paginacao.tamanho(tamanho));
    }

    @GetMapping("/produtores/{id}")
    @Operation(summary = "Detalhe de um produtor com contagens de ofertas e pedidos")
    public DetalheProdutorResponse detalheProdutor(@PathVariable Long id) {
        return adminService.detalheProdutor(id);
    }

    @PostMapping("/produtores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um produtor (já com e-mail confirmado)")
    public UsuarioResponse criarProdutor(@Valid @RequestBody CriarUtilizadorRequest req) {
        return adminService.criarProdutor(UsuarioAtual.obter().id(), req);
    }

    @PatchMapping("/produtores/{id}/estado")
    @Operation(summary = "Suspende (ativo=false) ou reactiva (ativo=true) um produtor")
    public UsuarioResponse alterarEstadoProdutor(@PathVariable Long id,
                                               @Valid @RequestBody AlterarEstadoContaRequest req) {
        return adminService.definirAtivoProdutor(UsuarioAtual.obter().id(), id, req.ativo());
    }

    @PatchMapping("/produtores/{id}/validar")
    @Operation(summary = "Valida manualmente a conta de um produtor (equivale a confirmar o e-mail)")
    public UsuarioResponse validarProdutor(@PathVariable Long id) {
        return adminService.validarProdutor(UsuarioAtual.obter().id(), id);
    }

    @DeleteMapping("/produtores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um produtor sem pedidos associados")
    public void removerProdutor(@PathVariable Long id) {
        adminService.removerProdutor(UsuarioAtual.obter().id(), id);
    }

    // ─── Compradores ─────────────────────────────────────────────────────

    @GetMapping("/compradores")
    @Operation(summary = "Lista/pesquisa compradores")
    public PageResponse<UsuarioResponse> listarCompradores(@RequestParam(required = false) String pesquisa,
                                                           @RequestParam(defaultValue = "0") int pagina,
                                                           @RequestParam(defaultValue = "20") int tamanho) {
        return adminService.listarCompradores(pesquisa, Paginacao.pagina(pagina), Paginacao.tamanho(tamanho));
    }

    @GetMapping("/compradores/{id}")
    @Operation(summary = "Detalhe de um comprador com contagens de pedidos")
    public DetalheCompradorResponse detalheComprador(@PathVariable Long id) {
        return adminService.detalheComprador(id);
    }

    @PostMapping("/compradores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um comprador (já com e-mail confirmado)")
    public UsuarioResponse criarComprador(@Valid @RequestBody CriarUtilizadorRequest req) {
        return adminService.criarComprador(UsuarioAtual.obter().id(), req);
    }

    @PatchMapping("/compradores/{id}/estado")
    @Operation(summary = "Suspende (ativo=false) ou reactiva (ativo=true) um comprador")
    public UsuarioResponse alterarEstadoComprador(@PathVariable Long id,
                                               @Valid @RequestBody AlterarEstadoContaRequest req) {
        return adminService.definirAtivoComprador(UsuarioAtual.obter().id(), id, req.ativo());
    }

    @PatchMapping("/compradores/{id}/validar")
    @Operation(summary = "Valida manualmente a conta de um comprador (equivale a confirmar o e-mail)")
    public UsuarioResponse validarComprador(@PathVariable Long id) {
        return adminService.validarComprador(UsuarioAtual.obter().id(), id);
    }

    @DeleteMapping("/compradores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um comprador sem pagamentos associados")
    public void removerComprador(@PathVariable Long id) {
        adminService.removerComprador(UsuarioAtual.obter().id(), id);
    }

    // ─── Vendas e histórico ──────────────────────────────────────────────

    @GetMapping("/vendas")
    @Operation(summary = "Todos os pedidos/vendas, com filtro opcional por estado")
    public PageResponse<PedidoResponse> vendas(@RequestParam(required = false) String estado,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int tamanho) {
        return adminService.vendas(estado, Paginacao.pagina(pagina), Paginacao.tamanho(tamanho));
    }

    @GetMapping("/historico")
    @Operation(summary = "Registo de auditoria (SuperAdmin vê tudo; os outros só as suas acções)")
    public PageResponse<AdminLogResponse> historico(@RequestParam(defaultValue = "0") int pagina,
                                                    @RequestParam(defaultValue = "20") int tamanho) {
        return adminService.historico(UsuarioAtual.obter(), UsuarioAtual.isSuperAdmin(),
            Paginacao.pagina(pagina), Paginacao.tamanho(tamanho));
    }

    // ─── Administradores (SUPER_ADMIN) ───────────────────────────────────

    @GetMapping("/administradores")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lista os outros administradores (SuperAdmin)")
    public List<UsuarioResponse> listarAdmins() {
        return adminService.listarAdmins(UsuarioAtual.obter().id());
    }

    @PostMapping("/administradores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cria um administrador (SuperAdmin)")
    public UsuarioResponse criarAdmin(@Valid @RequestBody CriarAdminRequest req) {
        return adminService.criarAdmin(UsuarioAtual.obter().id(), req);
    }

    @DeleteMapping("/administradores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Remove um administrador (SuperAdmin)")
    public void removerAdmin(@PathVariable Long id) {
        adminService.removerAdmin(UsuarioAtual.obter().id(), id);
    }
}
