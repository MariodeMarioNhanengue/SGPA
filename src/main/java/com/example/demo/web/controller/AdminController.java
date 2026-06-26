package com.example.demo.web.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.web.config.WebMvcConfig;
import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.AdminLog;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Pedido;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.repository.AdminLogRepository;
import com.example.demo.web.repository.AdminRepository;
import com.example.demo.web.repository.CompradorRepository;
import com.example.demo.web.repository.OfertaRepository;
import com.example.demo.web.repository.PedidoRepository;
import com.example.demo.web.repository.ProdutorRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String UPLOAD_DIR_PERFIL =
        WebMvcConfig.UPLOAD_BASE + java.io.File.separator + "images" + java.io.File.separator + "perfis";

    private final AdminRepository     adminRepository;
    private final ProdutorRepository  produtorRepository;
    private final CompradorRepository compradorRepository;
    private final PedidoRepository    pedidoRepository;
    private final OfertaRepository    ofertaRepository;
    private final AdminLogRepository  adminLogRepository;

    public AdminController(AdminRepository adminRepository,
                           ProdutorRepository produtorRepository,
                           CompradorRepository compradorRepository,
                           PedidoRepository pedidoRepository,
                           OfertaRepository ofertaRepository,
                           AdminLogRepository adminLogRepository) {
        this.adminRepository     = adminRepository;
        this.produtorRepository  = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.pedidoRepository    = pedidoRepository;
        this.ofertaRepository    = ofertaRepository;
        this.adminLogRepository  = adminLogRepository;
    }

    /** Regista uma acção do administrador no log. */
    private void registarLog(HttpSession session, String tipoAcao, String descricao,
                             String nomeAfectado, String tipoAfectado) {
        Long adminId = getAdminId(session);
        if (adminId == null) return;
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

    /** Sobrecarga para acções sem afectado (login, editar perfil) */
    private void registarLog(HttpSession session, String tipoAcao, String descricao) {
        registarLog(session, tipoAcao, descricao, null, null);
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("loggedInType"));
    }

    private boolean isSuperAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isSuperAdmin"));
    }

    private Long getAdminId(HttpSession session) {
        Object id = session.getAttribute("loggedInId");
        return id != null ? ((Number) id).longValue() : null;
    }

    // ════════ LOGIN / LOGOUT ════════

    @GetMapping("/login")
    public String loginPage() { return "admin/login-admin"; }

    @PostMapping("/login")
    public String processarLogin(@RequestParam String email,
                                 @RequestParam String senha,
                                 HttpSession session, Model model) {
        var opt = adminRepository.findByEmail(email);
        if (opt.isPresent()) {
            Admin a = opt.get();
            if (a.getSenha() != null && a.getSenha().equals(senha)) {
                a.setUltimoAcesso(java.time.LocalDateTime.now());
                adminRepository.save(a);
                session.setAttribute("loggedInType",  "ADMIN");
                session.setAttribute("loggedInId",    a.getId());
                session.setAttribute("loggedInNome",  a.getNomeCompleto());
                session.setAttribute("isSuperAdmin",  a.isSuperAdmin());
                session.setAttribute("adminFoto",     a.getFotoPerfil());
                // Registo de login (log manual pós-sessão)
                AdminLog logEntry = new AdminLog();
                logEntry.setAdmin(a);
                logEntry.setTipoAcao("LOGIN");
                logEntry.setDescricao("Sessão iniciada.");
                adminLogRepository.save(logEntry);
                return "redirect:/admin/dashboard";
            }
        }
        model.addAttribute("erro", "Credenciais inválidas. Verifique o e-mail e a senha.");
        return "admin/login-admin";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    // ════════ DASHBOARD ════════

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("nomeAdmin",       session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin",    isSuperAdmin(session));
        model.addAttribute("adminFoto",       session.getAttribute("adminFoto"));
        model.addAttribute("totalProdutores",  produtorRepository.count());
        model.addAttribute("totalCompradores", compradorRepository.count());
        model.addAttribute("totalPedidos",     pedidoRepository.count());
        model.addAttribute("totalOfertas",     ofertaRepository.count());
        model.addAttribute("pedidosPendentes", pedidoRepository.countPendentes());
        model.addAttribute("pedidosAceites",   pedidoRepository.countAceites());
        model.addAttribute("recentesPedidos",  pedidoRepository.findTop3ByOrderByDataPedidoDesc());
        return "admin/dashboard-admin";
    }

    // ════════ EDITAR PERFIL ════════

    @GetMapping("/perfil")
    public String editarPerfil(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Admin admin = adminRepository.findById(getAdminId(session))
            .orElseThrow(() -> new RuntimeException("Admin não encontrado."));
        model.addAttribute("admin",       admin);
        model.addAttribute("nomeAdmin",   session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", isSuperAdmin(session));
        model.addAttribute("adminFoto",   session.getAttribute("adminFoto"));
        return "admin/editar-perfil-admin";
    }

    @PostMapping("/perfil")
    public String salvarPerfil(@RequestParam String nome,
                               @RequestParam String apelido,
                               @RequestParam String email,
                               @RequestParam(required = false) String senhaAtual,
                               @RequestParam(required = false) String novaSenha,
                               @RequestParam(required = false) String confirmarSenha,
                               @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
                               HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Long id = getAdminId(session);
        Admin admin = adminRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Admin não encontrado."));

        adminRepository.findByEmail(email).ifPresent(existente -> {
            if (!existente.getId().equals(id))
                throw new RuntimeException("E-mail já em uso por outro administrador.");
        });

        admin.setNome(nome);
        admin.setApelido(apelido);
        admin.setEmail(email);

        if (novaSenha != null && !novaSenha.isBlank()) {
            if (!admin.getSenha().equals(senhaAtual)) {
                ra.addFlashAttribute("erro", "A senha actual está incorrecta.");
                return "redirect:/admin/perfil";
            }
            if (!novaSenha.equals(confirmarSenha)) {
                ra.addFlashAttribute("erro", "A nova senha e a confirmação não coincidem.");
                return "redirect:/admin/perfil";
            }
            admin.setSenha(novaSenha);
        }

        if (fotoPerfilFile != null && !fotoPerfilFile.isEmpty()) {
            try {
                String original = fotoPerfilFile.getOriginalFilename();
                String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf(".")) : ".jpg";
                String filename = "admin_" + id + "_" + UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR_PERFIL);
                Files.createDirectories(uploadPath);
                Files.copy(fotoPerfilFile.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                admin.setFotoPerfil("/uploads/images/perfis/" + filename);
            } catch (IOException e) { e.printStackTrace(); }
        }

        adminRepository.save(admin);
        session.setAttribute("loggedInNome", admin.getNomeCompleto());
        session.setAttribute("adminFoto",    admin.getFotoPerfil());
        registarLog(session, "EDITAR_PERFIL", "Actualizou o seu próprio perfil.");
        ra.addFlashAttribute("sucesso", "Perfil actualizado com sucesso!");
        return "redirect:/admin/perfil";
    }

    // ════════ HISTÓRICO DO ADMIN ════════

    @GetMapping("/historico")
    public String historico(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Long adminId = getAdminId(session);
        List<com.example.demo.web.domain.AdminLog> logs;
        if (isSuperAdmin(session)) {
            // SuperAdmin vê todos os logs de todos os administradores
            logs = adminLogRepository.findAllByOrderByDataAcaoDesc();
        } else {
            logs = adminLogRepository.findByAdminIdOrderByDataAcaoDesc(adminId);
        }
        model.addAttribute("nomeAdmin",    session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", isSuperAdmin(session));
        model.addAttribute("adminFoto",    session.getAttribute("adminFoto"));
        model.addAttribute("logs",         logs);
        return "admin/historico-admin";
    }

    // ════════ GESTÃO DE ADMINS (SUPERADMIN) ════════

    @GetMapping("/administradores")
    public String listarAdmins(HttpSession session, Model model) {
        if (!isAdmin(session))      return "redirect:/admin/login";
        Long myId = getAdminId(session);
        List<Admin> admins = adminRepository.findAll().stream()
            .filter(a -> !a.getId().equals(myId)).toList();
        model.addAttribute("nomeAdmin",    session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("adminFoto",    session.getAttribute("adminFoto"));
        model.addAttribute("admins",       admins);
        return "admin/gerir-admins";
    }

   @PostMapping("/administradores/criar")
public String criarAdmin(@RequestParam String nome,
                         @RequestParam String apelido,
                         @RequestParam String email,
                         @RequestParam String senha,
                         HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session))      return "redirect:/admin/login";
        if (adminRepository.existsByEmail(email)) {
            ra.addFlashAttribute("erro", "Já existe um administrador com este e-mail.");
            return "redirect:/admin/administradores";
        }
        Admin novo = new Admin();
        novo.setNome(nome); novo.setApelido(apelido);
        novo.setEmail(email); novo.setSenha(senha);
      novo.setSuperAdmin(false);
        adminRepository.save(novo);
        registarLog(session, "CRIAR_ADMIN", "Criou administrador: " + nome + " " + apelido + " (" + email + ")",
                    nome + " " + apelido, "ADMIN");
        ra.addFlashAttribute("sucesso", "Administrador " + nome + " " + apelido + " criado com sucesso.");
        return "redirect:/admin/administradores";
    }

    @PostMapping("/administradores/{id}/remover")
    public String removerAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session))      return "redirect:/admin/login";
        Admin target = adminRepository.findById(id).orElse(null);
        if (target != null && target.isSuperAdmin()) {
            ra.addFlashAttribute("erro", "Não é possível remover um SuperAdmin.");
            return "redirect:/admin/administradores";
        }
        adminRepository.deleteById(id);
        registarLog(session, "REMOVER_ADMIN", "Removeu administrador ID #" + id + (target != null ? " (" + target.getNomeCompleto() + ")" : ""),
                    target != null ? target.getNomeCompleto() : "ID #" + id, "ADMIN");
        ra.addFlashAttribute("sucesso", "Administrador removido com sucesso.");
        return "redirect:/admin/administradores";
    }

    // ════════ PRODUTORES ════════

    @GetMapping("/produtores")
    public String listarProdutores(@RequestParam(required = false) String pesquisa,
                                   HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        List<Produtor> produtores;
        if (pesquisa != null && !pesquisa.isBlank()) {
            String q = pesquisa.trim().toLowerCase();
            produtores = produtorRepository.findAll().stream()
                .filter(p -> p.getNome().toLowerCase().contains(q)
                          || p.getApelido().toLowerCase().contains(q)
                          || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))
                          || (p.getNuit() != null && p.getNuit().contains(q)))
                .toList();
        } else { produtores = produtorRepository.findAll(); }
        model.addAttribute("nomeAdmin",    session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", isSuperAdmin(session));
        model.addAttribute("adminFoto",    session.getAttribute("adminFoto"));
        model.addAttribute("produtores",   produtores);
        model.addAttribute("pesquisa",     pesquisa);
        return "admin/gerir-produtores";
    }

    @PostMapping("/produtores/{id}/remover")
    public String removerProdutor(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Produtor removido = produtorRepository.findById(id).orElse(null);
        produtorRepository.deleteById(id);
        registarLog(session, "REMOVER_PRODUTOR", "Removeu produtor ID #" + id + (removido != null ? " (" + removido.getNomeCompleto() + ")" : ""),
                    removido != null ? removido.getNomeCompleto() : "ID #" + id, "PRODUTOR");
        ra.addFlashAttribute("sucesso", "Produtor removido com sucesso.");
        return "redirect:/admin/produtores";
    }

    @PostMapping("/produtores/criar")
    public String criarProdutor(@RequestParam String nome,
                                @RequestParam String apelido,
                                @RequestParam String email,
                                @RequestParam(required = false) String telefone,
                                @RequestParam(required = false) String nuit,
                                @RequestParam String senha,
                                HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        if (produtorRepository.existsByEmail(email)) {
            ra.addFlashAttribute("erro", "Já existe um produtor com este e-mail.");
            return "redirect:/admin/produtores";
        }
        Produtor p = new Produtor();
        p.setNome(nome); p.setApelido(apelido);
        p.setEmail(email); p.setSenha(senha);
        if (telefone != null && !telefone.isBlank()) p.setTelefone(telefone);

        p.setNuit(
            (nuit == null || nuit.trim().isEmpty())
                ? null
                : nuit.trim()
        );
        produtorRepository.save(p);
        registarLog(session, "CRIAR_PRODUTOR", "Criou produtor: " + nome + " " + apelido + " (" + email + ")",
                    nome + " " + apelido, "PRODUTOR");
        ra.addFlashAttribute("sucesso", "Produtor " + nome + " " + apelido + " criado com sucesso.");
        return "redirect:/admin/produtores";
    }

    // ════════ COMPRADORES ════════

    @GetMapping("/compradores")
    public String listarCompradores(@RequestParam(required = false) String pesquisa,
                                    HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        List<Comprador> compradores;
        if (pesquisa != null && !pesquisa.isBlank()) {
            String q = pesquisa.trim().toLowerCase();
            compradores = compradorRepository.findAll().stream()
                .filter(c -> c.getNome().toLowerCase().contains(q)
                          || c.getApelido().toLowerCase().contains(q)
                          || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                          || (c.getNuit() != null && c.getNuit().contains(q)))
                .toList();
        } else { compradores = compradorRepository.findAll(); }
        model.addAttribute("nomeAdmin",    session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", isSuperAdmin(session));
        model.addAttribute("adminFoto",    session.getAttribute("adminFoto"));
        model.addAttribute("compradores",  compradores);
        model.addAttribute("pesquisa",     pesquisa);
        return "admin/gerir-compradores";
    }

    @PostMapping("/compradores/{id}/remover")
    public String removerComprador(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Comprador removidoC = compradorRepository.findById(id).orElse(null);
        compradorRepository.deleteById(id);
        registarLog(session, "REMOVER_COMPRADOR", "Removeu comprador ID #" + id + (removidoC != null ? " (" + removidoC.getNomeCompleto() + ")" : ""),
                    removidoC != null ? removidoC.getNomeCompleto() : "ID #" + id, "COMPRADOR");
        ra.addFlashAttribute("sucesso", "Comprador removido com sucesso.");
        return "redirect:/admin/compradores";
    }

    @PostMapping("/compradores/criar")
    public String criarComprador(@RequestParam String nome,
                                 @RequestParam String apelido,
                                 @RequestParam String email,
                                 @RequestParam String telefone,
                                 @RequestParam(required = false) String nuit,
                                 @RequestParam String senha,
                                 HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        if (compradorRepository.existsByEmail(email)) {
            ra.addFlashAttribute("erro", "Já existe um comprador com este e-mail.");
            return "redirect:/admin/compradores";
        }
        Comprador c = new Comprador();
        c.setNome(nome); c.setApelido(apelido);
        c.setEmail(email); c.setTelefone(telefone);
        c.setSenha(senha);

        c.setNuit(
            (nuit == null || nuit.trim().isEmpty())
                ? null
                : nuit.trim()
        );
        compradorRepository.save(c);
        registarLog(session, "CRIAR_COMPRADOR", "Criou comprador: " + nome + " " + apelido + " (" + email + ")",
                    nome + " " + apelido, "COMPRADOR");
        ra.addFlashAttribute("sucesso", "Comprador " + nome + " " + apelido + " criado com sucesso.");
        return "redirect:/admin/compradores";
    }

    // ════════ DETALHES COMPRADOR ════════

    @GetMapping("/compradores/{id}")
    public String verComprador(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Comprador c = compradorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Comprador não encontrado."));

        List<Pedido> pedidos = c.getPedidos() != null ? c.getPedidos() : java.util.List.of();
        long aceites    = pedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.ACEITE).count();
        long pendentes  = pedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.PENDENTE).count();
        long rejeitados = pedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.REJEITADO).count();

        model.addAttribute("nomeAdmin",    session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", isSuperAdmin(session));
        model.addAttribute("adminFoto",    session.getAttribute("adminFoto"));
        model.addAttribute("comprador",    c);
        model.addAttribute("pedidos",      pedidos);
        model.addAttribute("totalPedidos", pedidos.size());
        model.addAttribute("aceites",      aceites);
        model.addAttribute("pendentes",    pendentes);
        model.addAttribute("rejeitados",   rejeitados);
        return "admin/detalhe-comprador";
    }

    // ════════ DETALHES PRODUTOR ════════

    @GetMapping("/produtores/{id}")
    public String verProdutor(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Produtor p = produtorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produtor não encontrado."));

        List<com.example.demo.web.domain.Oferta> ofertas = p.getOfertas() != null ? p.getOfertas() : java.util.List.of();
        long ofertasAtivas    = ofertas.stream().filter(o -> o.getEstado() == com.example.demo.web.domain.Oferta.EstadoOferta.ATIVO).count();
        long ofertasExpiradas = ofertas.stream().filter(o -> o.getEstado() == com.example.demo.web.domain.Oferta.EstadoOferta.EXPIRADO).count();
        long ofertasInativas  = ofertas.stream().filter(o -> o.getEstado() == com.example.demo.web.domain.Oferta.EstadoOferta.INATIVO).count();

        // Pedidos recebidos por este produtor (através das suas ofertas)
        List<Pedido> pedidosProdutor = pedidoRepository.findByProdutorId(id);
        long pedAceites    = pedidosProdutor.stream().filter(pe -> pe.getEstado() == Pedido.EstadoPedido.ACEITE).count();
        long pedPendentes  = pedidosProdutor.stream().filter(pe -> pe.getEstado() == Pedido.EstadoPedido.PENDENTE).count();
        long pedRejeitados = pedidosProdutor.stream().filter(pe -> pe.getEstado() == Pedido.EstadoPedido.REJEITADO).count();

        model.addAttribute("nomeAdmin",        session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin",     isSuperAdmin(session));
        model.addAttribute("adminFoto",        session.getAttribute("adminFoto"));
        model.addAttribute("produtor",         p);
        model.addAttribute("ofertas",          ofertas);
        model.addAttribute("totalOfertas",     ofertas.size());
        model.addAttribute("ofertasAtivas",    ofertasAtivas);
        model.addAttribute("ofertasExpiradas", ofertasExpiradas);
        model.addAttribute("ofertasInativas",  ofertasInativas);
        model.addAttribute("pedidosProdutor",  pedidosProdutor);
        model.addAttribute("totalPedidosProdutor", pedidosProdutor.size());
        model.addAttribute("pedAceites",       pedAceites);
        model.addAttribute("pedPendentes",     pedPendentes);
        model.addAttribute("pedRejeitados",    pedRejeitados);
        return "admin/detalhe-produtor";
    }

    // ════════ VENDAS ════════

    @GetMapping("/vendas")
    public String listarVendas(@RequestParam(required = false) String estado,
                               HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        List<Pedido> pedidos;
        if (estado != null && !estado.isBlank()) {
            try { pedidos = pedidoRepository.findByEstado(Pedido.EstadoPedido.valueOf(estado.toUpperCase())); }
            catch (IllegalArgumentException e) { pedidos = pedidoRepository.findAll(); }
        } else { pedidos = pedidoRepository.findAll(); }
        model.addAttribute("nomeAdmin",    session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", isSuperAdmin(session));
        model.addAttribute("adminFoto",    session.getAttribute("adminFoto"));
        model.addAttribute("pedidos",      pedidos);
        model.addAttribute("estadoFiltro", estado);
        return "admin/ver-vendas";
    }
}
