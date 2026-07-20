package com.example.demo.web.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.AdminLog;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Oferta;
import com.example.demo.web.domain.Pedido;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.repository.AdminLogRepository;
import com.example.demo.web.repository.AdminRepository;
import com.example.demo.web.repository.CompradorRepository;
import com.example.demo.web.repository.OfertaRepository;
import com.example.demo.web.repository.PedidoRepository;
import com.example.demo.web.repository.ProdutorRepository;
import com.example.demo.web.service.exception.RecursoNaoEncontradoException;
import com.example.demo.web.service.exception.RegraNegocioException;

import jakarta.servlet.http.HttpSession;

/**
 * Regras de negócio da área de administração: estatísticas do dashboard,
 * gestão de perfil, gestão de produtores/compradores/administradores e
 * consulta de vendas. As permissões (isAdmin/isSuperAdmin) e o log de
 * auditoria são delegados ao {@link AdminAuthService}.
 */
@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final PedidoRepository pedidoRepository;
    private final OfertaRepository ofertaRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminAuthService adminAuthService;
    private final ArmazenamentoService armazenamentoService;
    private final PasswordEncoderService passwordEncoderService;

    public AdminService(AdminRepository adminRepository,
                        ProdutorRepository produtorRepository,
                        CompradorRepository compradorRepository,
                        PedidoRepository pedidoRepository,
                        OfertaRepository ofertaRepository,
                        AdminLogRepository adminLogRepository,
                        AdminAuthService adminAuthService,
                        ArmazenamentoService armazenamentoService,
                        PasswordEncoderService passwordEncoderService) {
        this.adminRepository = adminRepository;
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.pedidoRepository = pedidoRepository;
        this.ofertaRepository = ofertaRepository;
        this.adminLogRepository = adminLogRepository;
        this.adminAuthService = adminAuthService;
        this.armazenamentoService = armazenamentoService;
        this.passwordEncoderService = passwordEncoderService;
    }

    // ─── DASHBOARD ───────────────────────────────────────────────────────

    public record EstatisticasDashboard(long totalProdutores, long totalCompradores, long totalPedidos,
                                        long totalOfertas, long pedidosPendentes, long pedidosAceites,
                                        List<Pedido> recentesPedidos) {}

    public EstatisticasDashboard estatisticasDashboard() {
        return new EstatisticasDashboard(
            produtorRepository.count(),
            compradorRepository.count(),
            pedidoRepository.count(),
            ofertaRepository.count(),
            pedidoRepository.countPendentes(),
            pedidoRepository.countAceites(),
            pedidoRepository.findTop3ByOrderByDataPedidoDesc()
        );
    }

    // ─── PERFIL ──────────────────────────────────────────────────────────

    public Admin obterAdmin(Long id) {
        return adminRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Admin não encontrado."));
    }

    public Admin atualizarPerfil(Long id, String nome, String apelido, String email,
                                 String senhaAtual, String novaSenha, String confirmarSenha,
                                 MultipartFile fotoPerfilFile, HttpSession session) {
        Admin admin = obterAdmin(id);

        adminRepository.findByEmail(email).ifPresent(existente -> {
            if (!existente.getId().equals(id))
                throw new RegraNegocioException("E-mail já em uso por outro administrador.");
        });

        admin.setNome(nome);
        admin.setApelido(apelido);
        admin.setEmail(email);

        if (novaSenha != null && !novaSenha.isBlank()) {
            if (!passwordEncoderService.matches(senhaAtual, admin.getSenha())) {
                throw new RegraNegocioException("A senha actual está incorrecta.");
            }
            if (!novaSenha.equals(confirmarSenha)) {
                throw new RegraNegocioException("A nova senha e a confirmação não coincidem.");
            }
            admin.setSenha(passwordEncoderService.encode(novaSenha));
        }

        String novaFoto = armazenamentoService.guardarFotoPerfil(fotoPerfilFile, "admin", id);
        if (novaFoto != null) admin.setFotoPerfil(novaFoto);

        adminRepository.save(admin);
        adminAuthService.atualizarSessao(session, admin);
        adminAuthService.registarLog(session, "EDITAR_PERFIL", "Actualizou o seu próprio perfil.");
        return admin;
    }

    // ─── HISTÓRICO ───────────────────────────────────────────────────────

    /** Devolve os logs visíveis para o admin autenticado: todos se for SuperAdmin, apenas os seus caso contrário. */
    public List<AdminLog> historico(HttpSession session) {
        if (adminAuthService.isSuperAdmin(session)) {
            return adminLogRepository.findAllByOrderByDataAcaoDesc();
        }
        return adminLogRepository.findByAdminIdOrderByDataAcaoDesc(adminAuthService.getAdminId(session));
    }

    // ─── GESTÃO DE ADMINS ────────────────────────────────────────────────

    public List<Admin> listarAdmins(HttpSession session) {
        Long myId = adminAuthService.getAdminId(session);
        return adminRepository.findAll().stream()
            .filter(a -> !a.getId().equals(myId)).toList();
    }

    public Admin criarAdmin(String nome, String apelido, String email, String senha, HttpSession session) {
        if (adminRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um administrador com este e-mail.");
        }
        Admin novo = new Admin();
        novo.setNome(nome);
        novo.setApelido(apelido);
        novo.setEmail(email);
        novo.setSenha(passwordEncoderService.encode(senha));
        novo.setSuperAdmin(false);
        adminRepository.save(novo);
        adminAuthService.registarLog(session, "CRIAR_ADMIN",
            "Criou administrador: " + nome + " " + apelido + " (" + email + ")",
            nome + " " + apelido, "ADMIN");
        return novo;
    }

    public void removerAdmin(Long id, HttpSession session) {
        Admin target = adminRepository.findById(id).orElse(null);
        if (target != null && target.isSuperAdmin()) {
            throw new RegraNegocioException("Não é possível remover um SuperAdmin.");
        }
        adminRepository.deleteById(id);
        adminAuthService.registarLog(session, "REMOVER_ADMIN",
            "Removeu administrador ID #" + id + (target != null ? " (" + target.getNomeCompleto() + ")" : ""),
            target != null ? target.getNomeCompleto() : "ID #" + id, "ADMIN");
    }

    // ─── PRODUTORES ──────────────────────────────────────────────────────

    public List<Produtor> listarProdutores(String pesquisa) {
        if (pesquisa == null || pesquisa.isBlank()) return produtorRepository.findAll();
        String q = pesquisa.trim().toLowerCase();
        return produtorRepository.findAll().stream()
            .filter(p -> p.getNome().toLowerCase().contains(q)
                      || p.getApelido().toLowerCase().contains(q)
                      || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))
                      || (p.getNuit() != null && p.getNuit().contains(q)))
            .toList();
    }

    public void removerProdutor(Long id, HttpSession session) {
        Produtor removido = produtorRepository.findById(id).orElse(null);
        produtorRepository.deleteById(id);
        adminAuthService.registarLog(session, "REMOVER_PRODUTOR",
            "Removeu produtor ID #" + id + (removido != null ? " (" + removido.getNomeCompleto() + ")" : ""),
            removido != null ? removido.getNomeCompleto() : "ID #" + id, "PRODUTOR");
    }

    public Produtor criarProdutor(String nome, String apelido, String email, String telefone,
                                  String nuit, String senha, HttpSession session) {
        if (produtorRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um produtor com este e-mail.");
        }
        Produtor p = new Produtor();
        p.setNome(nome);
        p.setApelido(apelido);
        p.setEmail(email);
        p.setSenha(passwordEncoderService.encode(senha));
        if (telefone != null && !telefone.isBlank()) p.setTelefone(telefone);
        p.setNuit((nuit == null || nuit.trim().isEmpty()) ? null : nuit.trim());
        produtorRepository.save(p);
        adminAuthService.registarLog(session, "CRIAR_PRODUTOR",
            "Criou produtor: " + nome + " " + apelido + " (" + email + ")",
            nome + " " + apelido, "PRODUTOR");
        return p;
    }

    public record DetalheProdutor(Produtor produtor, List<Oferta> ofertas, long totalOfertas,
                                  long ofertasAtivas, long ofertasExpiradas, long ofertasInativas,
                                  List<Pedido> pedidosProdutor, long totalPedidosProdutor,
                                  long pedAceites, long pedPendentes, long pedRejeitados) {}

    public DetalheProdutor detalheProdutor(Long id) {
        Produtor p = produtorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));

        List<Oferta> ofertas = p.getOfertas() != null ? p.getOfertas() : List.of();
        long ativas = ofertas.stream().filter(o -> o.getEstado() == Oferta.EstadoOferta.ATIVO).count();
        long expiradas = ofertas.stream().filter(o -> o.getEstado() == Oferta.EstadoOferta.EXPIRADO).count();
        long inativas = ofertas.stream().filter(o -> o.getEstado() == Oferta.EstadoOferta.INATIVO).count();

        List<Pedido> pedidosProdutor = pedidoRepository.findByProdutorId(id);
        long aceites = pedidosProdutor.stream().filter(pe -> pe.getEstado() == Pedido.EstadoPedido.ACEITE).count();
        long pendentes = pedidosProdutor.stream().filter(pe -> pe.getEstado() == Pedido.EstadoPedido.PENDENTE).count();
        long rejeitados = pedidosProdutor.stream().filter(pe -> pe.getEstado() == Pedido.EstadoPedido.REJEITADO).count();

        return new DetalheProdutor(p, ofertas, ofertas.size(), ativas, expiradas, inativas,
            pedidosProdutor, pedidosProdutor.size(), aceites, pendentes, rejeitados);
    }

    // ─── COMPRADORES ─────────────────────────────────────────────────────

    public List<Comprador> listarCompradores(String pesquisa) {
        if (pesquisa == null || pesquisa.isBlank()) return compradorRepository.findAll();
        String q = pesquisa.trim().toLowerCase();
        return compradorRepository.findAll().stream()
            .filter(c -> c.getNome().toLowerCase().contains(q)
                      || c.getApelido().toLowerCase().contains(q)
                      || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                      || (c.getNuit() != null && c.getNuit().contains(q)))
            .toList();
    }

    public void removerComprador(Long id, HttpSession session) {
        Comprador removido = compradorRepository.findById(id).orElse(null);
        compradorRepository.deleteById(id);
        adminAuthService.registarLog(session, "REMOVER_COMPRADOR",
            "Removeu comprador ID #" + id + (removido != null ? " (" + removido.getNomeCompleto() + ")" : ""),
            removido != null ? removido.getNomeCompleto() : "ID #" + id, "COMPRADOR");
    }

    public Comprador criarComprador(String nome, String apelido, String email, String telefone,
                                    String nuit, String senha, HttpSession session) {
        if (compradorRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um comprador com este e-mail.");
        }
        Comprador c = new Comprador();
        c.setNome(nome);
        c.setApelido(apelido);
        c.setEmail(email);
        c.setTelefone(telefone);
        c.setSenha(passwordEncoderService.encode(senha));
        c.setNuit((nuit == null || nuit.trim().isEmpty()) ? null : nuit.trim());
        compradorRepository.save(c);
        adminAuthService.registarLog(session, "CRIAR_COMPRADOR",
            "Criou comprador: " + nome + " " + apelido + " (" + email + ")",
            nome + " " + apelido, "COMPRADOR");
        return c;
    }

    public record DetalheComprador(Comprador comprador, List<Pedido> pedidos, long totalPedidos,
                                   long aceites, long pendentes, long rejeitados) {}

    public DetalheComprador detalheComprador(Long id) {
        Comprador c = compradorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));

        List<Pedido> pedidos = c.getPedidos() != null ? c.getPedidos() : List.of();
        long aceites = pedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.ACEITE).count();
        long pendentes = pedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.PENDENTE).count();
        long rejeitados = pedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.REJEITADO).count();

        return new DetalheComprador(c, pedidos, pedidos.size(), aceites, pendentes, rejeitados);
    }

    // ─── VENDAS ──────────────────────────────────────────────────────────

    public List<Pedido> listarVendas(String estado) {
        if (estado == null || estado.isBlank()) return pedidoRepository.findAll();
        try {
            return pedidoRepository.findByEstado(Pedido.EstadoPedido.valueOf(estado.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return pedidoRepository.findAll();
        }
    }
}
