package com.sgpa.mario.api.service;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgpa.mario.api.config.UsuarioAtual;
import com.sgpa.mario.api.dto.AdminDtos.AdminLogResponse;
import com.sgpa.mario.api.dto.AdminDtos.CriarAdminRequest;
import com.sgpa.mario.api.dto.AdminDtos.CriarUtilizadorRequest;
import com.sgpa.mario.api.dto.AdminDtos.DetalheCompradorResponse;
import com.sgpa.mario.api.dto.AdminDtos.DetalheProdutorResponse;
import com.sgpa.mario.api.dto.AdminDtos.ResumoAdminResponse;
import com.sgpa.mario.api.dto.PageResponse;
import com.sgpa.mario.api.dto.PedidoDtos.PedidoResponse;
import com.sgpa.mario.api.dto.UsuarioDtos.UsuarioResponse;
import com.sgpa.mario.api.exception.ApiException;
import com.sgpa.mario.web.domain.Admin;
import com.sgpa.mario.web.domain.AdminLog;
import com.sgpa.mario.web.domain.Comprador;
import com.sgpa.mario.web.domain.Pedido.EstadoPedido;
import com.sgpa.mario.web.domain.Produtor;
import com.sgpa.mario.api.repository.AdminLogRepository;
import com.sgpa.mario.api.repository.AdminRepository;
import com.sgpa.mario.api.repository.CompradorRepository;
import com.sgpa.mario.api.repository.PedidoRepository;
import com.sgpa.mario.api.repository.ProdutorRepository;

import com.sgpa.mario.api.service.exception.RecursoNaoEncontradoException;
import com.sgpa.mario.api.service.exception.RegraNegocioException;

/** Operações da área de administração (sem sessão), com registo de auditoria em admin_log. */
@Service
public class ApiAdminService {

    private final AdminRepository adminRepository;
    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final PedidoRepository pedidoRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminService adminService;
    private final ApiPedidoService pedidos;
    private final PasswordEncoderService passwords;

    public ApiAdminService(AdminRepository adminRepository, ProdutorRepository produtorRepository,
                           CompradorRepository compradorRepository, PedidoRepository pedidoRepository,
                           AdminLogRepository adminLogRepository, AdminService adminService,
                           ApiPedidoService pedidos, PasswordEncoderService passwords) {
        this.adminRepository = adminRepository;
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.pedidoRepository = pedidoRepository;
        this.adminLogRepository = adminLogRepository;
        this.adminService = adminService;
        this.pedidos = pedidos;
        this.passwords = passwords;
    }

    // ─── Resumo ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResumoAdminResponse resumo() {
        var e = adminService.estatisticasDashboard();
        return new ResumoAdminResponse(e.totalProdutores(), e.totalCompradores(), e.totalPedidos(),
            e.totalOfertas(), e.pedidosPendentes(), e.pedidosAceites(), pedidos.mapear(e.recentesPedidos()));
    }

    // ─── Produtores ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listarProdutores(String pesquisa, int pagina, int tamanho) {
        List<UsuarioResponse> lista = adminService.listarProdutores(pesquisa).stream()
            .map(p -> ApiMapper.produtor(p, null)).toList();
        return PageResponse.de(lista, pagina, tamanho);
    }

    @Transactional(readOnly = true)
    public DetalheProdutorResponse detalheProdutor(Long id) {
        var d = adminService.detalheProdutor(id);
        return new DetalheProdutorResponse(ApiMapper.produtor(d.produtor(), null),
            d.totalOfertas(), d.ofertasAtivas(), d.ofertasExpiradas(), d.ofertasInativas(),
            d.totalPedidosProdutor(), d.pedAceites(), d.pedPendentes(), d.pedRejeitados());
    }

    @Transactional
    public UsuarioResponse criarProdutor(Long adminId, CriarUtilizadorRequest r) {
        String email = r.email().trim();
        if (produtorRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um produtor com este e-mail.");
        }
        String nuit = vazioParaNull(r.nuit());
        if (nuit != null && produtorRepository.existsByNuit(nuit)) {
            throw new RegraNegocioException("Este NUIT já está registado por outro produtor.");
        }
        Produtor p = new Produtor();
        p.setNome(r.nome().trim());
        p.setApelido(r.apelido().trim());
        p.setEmail(email);
        p.setTelefone(vazioParaNull(r.telefone()));
        p.setNuit(nuit);
        p.setSenha(passwords.encode(r.senha()));
        p.setEmailVerificado(Boolean.TRUE);
        p.setAtivo(Boolean.TRUE);
        produtorRepository.save(p);
        registarLog(adminId, "CRIAR_PRODUTOR", "Criou produtor: " + p.getNomeCompleto() + " (" + email + ")",
            p.getNomeCompleto(), "PRODUTOR");
        return ApiMapper.produtor(p, null);
    }

    /** Sem @Transactional: o erro de integridade (pedidos associados) surge aqui e vira 409. */
    public void removerProdutor(Long adminId, Long id) {
        Produtor p = produtorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
        String nome = p.getNomeCompleto();
        try {
            produtorRepository.delete(p);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "UTILIZADOR_COM_DADOS",
                "Este produtor tem pedidos associados e não pode ser removido. Suspenda a conta em vez de a remover.");
        }
        registarLog(adminId, "REMOVER_PRODUTOR", "Removeu produtor ID #" + id + " (" + nome + ")", nome, "PRODUTOR");
    }

    @Transactional
    public UsuarioResponse definirAtivoProdutor(Long adminId, Long id, boolean ativo) {
        Produtor p = produtorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
        p.setAtivo(ativo);
        produtorRepository.save(p);
        registarLog(adminId, ativo ? "ATIVAR_PRODUTOR" : "SUSPENDER_PRODUTOR",
            (ativo ? "Reactivou" : "Suspendeu") + " o produtor " + p.getNomeCompleto(), p.getNomeCompleto(), "PRODUTOR");
        return ApiMapper.produtor(p, null);
    }

    @Transactional
    public UsuarioResponse validarProdutor(Long adminId, Long id) {
        Produtor p = produtorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
        if (!Boolean.FALSE.equals(p.getEmailVerificado())) {
            throw new RegraNegocioException("Esta conta já está validada.");
        }
        p.setEmailVerificado(Boolean.TRUE);
        produtorRepository.save(p);
        registarLog(adminId, "VALIDAR_PRODUTOR",
            "Validou manualmente a conta do produtor " + p.getNomeCompleto() + " (" + p.getEmail() + ")",
            p.getNomeCompleto(), "PRODUTOR");
        return ApiMapper.produtor(p, null);
    }

    // ─── Compradores ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listarCompradores(String pesquisa, int pagina, int tamanho) {
        List<UsuarioResponse> lista = adminService.listarCompradores(pesquisa).stream()
            .map(c -> ApiMapper.comprador(c, null)).toList();
        return PageResponse.de(lista, pagina, tamanho);
    }

    @Transactional(readOnly = true)
    public DetalheCompradorResponse detalheComprador(Long id) {
        var d = adminService.detalheComprador(id);
        return new DetalheCompradorResponse(ApiMapper.comprador(d.comprador(), null),
            d.totalPedidos(), d.aceites(), d.pendentes(), d.rejeitados());
    }

    @Transactional
    public UsuarioResponse criarComprador(Long adminId, CriarUtilizadorRequest r) {
        String email = r.email().trim();
        if (compradorRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um comprador com este e-mail.");
        }
        String telefone = vazioParaNull(r.telefone());
        if (telefone == null) {
            throw new RegraNegocioException("O telefone é obrigatório para compradores.");
        }
        String nuit = vazioParaNull(r.nuit());
        if (nuit != null && compradorRepository.existsByNuit(nuit)) {
            throw new RegraNegocioException("Este NUIT já está registado por outro comprador.");
        }
        Comprador c = new Comprador();
        c.setNome(r.nome().trim());
        c.setApelido(r.apelido().trim());
        c.setEmail(email);
        c.setTelefone(telefone);
        c.setNuit(nuit);
        c.setSenha(passwords.encode(r.senha()));
        c.setEmailVerificado(Boolean.TRUE);
        c.setAtivo(Boolean.TRUE);
        compradorRepository.save(c);
        registarLog(adminId, "CRIAR_COMPRADOR", "Criou comprador: " + c.getNomeCompleto() + " (" + email + ")",
            c.getNomeCompleto(), "COMPRADOR");
        return ApiMapper.comprador(c, null);
    }

    /** Sem @Transactional: o erro de integridade surge aqui e vira 409. */
    public void removerComprador(Long adminId, Long id) {
        Comprador c = compradorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));
        String nome = c.getNomeCompleto();
        try {
            compradorRepository.delete(c);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "UTILIZADOR_COM_DADOS",
                "Este comprador tem pagamentos associados e não pode ser removido. Suspenda a conta em vez de a remover.");
        }
        registarLog(adminId, "REMOVER_COMPRADOR", "Removeu comprador ID #" + id + " (" + nome + ")", nome, "COMPRADOR");
    }

    @Transactional
    public UsuarioResponse definirAtivoComprador(Long adminId, Long id, boolean ativo) {
        Comprador c = compradorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));
        c.setAtivo(ativo);
        compradorRepository.save(c);
        registarLog(adminId, ativo ? "ATIVAR_COMPRADOR" : "SUSPENDER_COMPRADOR",
            (ativo ? "Reactivou" : "Suspendeu") + " o comprador " + c.getNomeCompleto(), c.getNomeCompleto(), "COMPRADOR");
        return ApiMapper.comprador(c, null);
    }

    @Transactional
    public UsuarioResponse validarComprador(Long adminId, Long id) {
        Comprador c = compradorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));
        if (!Boolean.FALSE.equals(c.getEmailVerificado())) {
            throw new RegraNegocioException("Esta conta já está validada.");
        }
        c.setEmailVerificado(Boolean.TRUE);
        compradorRepository.save(c);
        registarLog(adminId, "VALIDAR_COMPRADOR",
            "Validou manualmente a conta do comprador " + c.getNomeCompleto() + " (" + c.getEmail() + ")",
            c.getNomeCompleto(), "COMPRADOR");
        return ApiMapper.comprador(c, null);
    }

    // ─── Vendas / pedidos ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> vendas(String estado, int pagina, int tamanho) {
        List<com.sgpa.mario.web.domain.Pedido> lista;
        if (estado == null || estado.isBlank()) {
            lista = pedidoRepository.findAll();
        } else {
            try {
                lista = pedidoRepository.findByEstado(EstadoPedido.valueOf(estado.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO",
                    "Estado inválido. Valores possíveis: PENDENTE, AGUARDANDO_PAGAMENTO, ACEITE, REJEITADO, CANCELADO.");
            }
        }
        return PageResponse.de(pedidos.mapear(lista), pagina, tamanho);
    }

    // ─── Histórico de auditoria ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AdminLogResponse> historico(UsuarioAtual.Dados admin, boolean superAdmin, int pagina, int tamanho) {
        List<AdminLog> logs = superAdmin
            ? adminLogRepository.findAllByOrderByDataAcaoDesc()
            : adminLogRepository.findByAdminIdOrderByDataAcaoDesc(admin.id());
        List<AdminLogResponse> lista = logs.stream()
            .map(l -> new AdminLogResponse(l.getId(),
                l.getAdmin() == null ? null : l.getAdmin().getNomeCompleto(),
                l.getTipoAcao(), l.getDescricao(), l.getNomeAfectado(), l.getTipoAfectado(), l.getDataAcao()))
            .toList();
        return PageResponse.de(lista, pagina, tamanho);
    }

    // ─── Administradores (SUPER_ADMIN) ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarAdmins(Long meuId) {
        return adminRepository.findAll().stream()
            .filter(a -> !a.getId().equals(meuId))
            .map(ApiMapper::admin).toList();
    }

    @Transactional
    public UsuarioResponse criarAdmin(Long adminId, CriarAdminRequest r) {
        String email = r.email().trim();
        if (adminRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um administrador com este e-mail.");
        }
        Admin novo = new Admin();
        novo.setNome(r.nome().trim());
        novo.setApelido(r.apelido().trim());
        novo.setEmail(email);
        novo.setSenha(passwords.encode(r.senha()));
        novo.setSuperAdmin(false);
        adminRepository.save(novo);
        registarLog(adminId, "CRIAR_ADMIN", "Criou administrador: " + novo.getNomeCompleto() + " (" + email + ")",
            novo.getNomeCompleto(), "ADMIN");
        return ApiMapper.admin(novo);
    }

    /** Sem @Transactional: administradores com histórico de acções não podem ser apagados (409). */
    public void removerAdmin(Long adminId, Long id) {
        if (adminId.equals(id)) {
            throw new RegraNegocioException("Não pode remover a sua própria conta.");
        }
        Admin alvo = adminRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador não encontrado."));
        if (alvo.isSuperAdmin()) {
            throw new RegraNegocioException("Não é possível remover um SuperAdmin.");
        }
        String nome = alvo.getNomeCompleto();
        try {
            adminRepository.delete(alvo);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_COM_HISTORICO",
                "Este administrador tem histórico de acções registado e não pode ser removido.");
        }
        registarLog(adminId, "REMOVER_ADMIN", "Removeu administrador ID #" + id + " (" + nome + ")", nome, "ADMIN");
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private void registarLog(Long adminId, String tipoAcao, String descricao, String nomeAfectado, String tipoAfectado) {
        adminRepository.findById(adminId).ifPresent(admin -> {
            AdminLog log = new AdminLog();
            log.setAdmin(admin);
            log.setTipoAcao(tipoAcao);
            log.setDescricao(descricao);
            log.setNomeAfectado(nomeAfectado);
            log.setTipoAfectado(tipoAfectado);
            adminLogRepository.save(log);
        });
    }

    private static String vazioParaNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
