package com.sgpa.mario.api.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sgpa.mario.web.domain.Admin;
import com.sgpa.mario.api.service.AdminAuthService;
import com.sgpa.mario.api.service.AdminService;
import com.sgpa.mario.api.service.exception.RegraNegocioException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;

    public AdminController(AdminAuthService adminAuthService, AdminService adminService) {
        this.adminAuthService = adminAuthService;
        this.adminService = adminService;
    }

    /** Popula os atributos comuns ao layout da área de administração. */
    private void preencherModeloBase(Model model, HttpSession session) {
        model.addAttribute("nomeAdmin", session.getAttribute("loggedInNome"));
        model.addAttribute("isSuperAdmin", adminAuthService.isSuperAdmin(session));
        model.addAttribute("adminFoto", session.getAttribute("adminFoto"));
    }

    private String negarGestaoAdmins(RedirectAttributes ra) {
        ra.addFlashAttribute("erro", AdminService.MSG_SO_SUPER_ADMIN);
        return "redirect:/admin/dashboard";
    }

    // ════════ LOGIN / LOGOUT ════════

    @GetMapping("/login")
    public String loginPage() { return "admin/login-admin"; }

    @PostMapping("/login")
    public String processarLogin(@RequestParam String email,
                                 @RequestParam String senha,
                                 HttpSession session, Model model) {
        AdminAuthService.ResultadoLoginAdmin resultado = adminAuthService.autenticar(email, senha, session);

        if (resultado.sucesso()) {
            return "redirect:/admin/dashboard";
        }
        if (resultado.bloqueado()) {
            model.addAttribute("erro", "Demasiadas tentativas falhadas. Tente novamente dentro de "
                + resultado.minutosRestantes() + " minuto(s).");
        } else {
            model.addAttribute("erro", "Credenciais inválidas. Verifique o e-mail e a senha.");
        }
        return "admin/login-admin";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        adminAuthService.logout(session);
        return "redirect:/admin/login";
    }

    // ════════ DASHBOARD ════════

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        preencherModeloBase(model, session);
        var stats = adminService.estatisticasDashboard();
        model.addAttribute("totalProdutores",  stats.totalProdutores());
        model.addAttribute("totalCompradores", stats.totalCompradores());
        model.addAttribute("totalPedidos",     stats.totalPedidos());
        model.addAttribute("totalOfertas",     stats.totalOfertas());
        model.addAttribute("pedidosPendentes", stats.pedidosPendentes());
        model.addAttribute("pedidosAceites",   stats.pedidosAceites());
        model.addAttribute("recentesPedidos",  stats.recentesPedidos());
        return "admin/dashboard-admin";
    }

    // ════════ EDITAR PERFIL ════════

    @GetMapping("/perfil")
    public String editarPerfil(HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin", adminService.obterAdmin(adminAuthService.getAdminId(session)));
        preencherModeloBase(model, session);
        return "admin/editar-perfil-admin";
    }

    @PutMapping("/perfil")
    public String salvarPerfil(@RequestParam String nome,
                               @RequestParam String apelido,
                               @RequestParam String email,
                               @RequestParam(required = false) String senhaAtual,
                               @RequestParam(required = false) String novaSenha,
                               @RequestParam(required = false) String confirmarSenha,
                               @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
                               HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        try {
            adminService.atualizarPerfil(adminAuthService.getAdminId(session), nome, apelido, email,
                senhaAtual, novaSenha, confirmarSenha, fotoPerfilFile, session);
            ra.addFlashAttribute("sucesso", "Perfil actualizado com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/perfil";
    }

    // ════════ HISTÓRICO DO ADMIN ════════

    @GetMapping("/historico")
    public String historico(HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        preencherModeloBase(model, session);
        model.addAttribute("logs", adminService.historico(session));
        return "admin/historico-admin";
    }

    // ════════ GESTÃO DE ADMINS (SUPERADMIN) ════════

    // Só o Super Admin cria/remove/lista administradores; um Admin normal é mandado de volta ao painel.
    @GetMapping("/administradores")
    public String listarAdmins(HttpSession session, Model model, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        if (!adminAuthService.isSuperAdmin(session)) return negarGestaoAdmins(ra);
        preencherModeloBase(model, session);
        model.addAttribute("admins", adminService.listarAdmins(session));
        return "admin/gerir-admins";
    }

    @PostMapping("/administradores/criar")
    public String criarAdmin(@RequestParam String nome,
                             @RequestParam String apelido,
                             @RequestParam String email,
                             @RequestParam String senha,
                             HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        if (!adminAuthService.isSuperAdmin(session)) return negarGestaoAdmins(ra);
        try {
            Admin novo = adminService.criarAdmin(nome, apelido, email, senha, session);
            ra.addFlashAttribute("sucesso", "Administrador " + novo.getNomeCompleto() + " criado com sucesso.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/administradores";
    }

    @DeleteMapping("/administradores/{id}")
    public String removerAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        if (!adminAuthService.isSuperAdmin(session)) return negarGestaoAdmins(ra);
        try {
            adminService.removerAdmin(id, session);
            ra.addFlashAttribute("sucesso", "Administrador removido com sucesso.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/administradores";
    }

    // ════════ PRODUTORES ════════

    @GetMapping("/produtores")
    public String listarProdutores(@RequestParam(required = false) String pesquisa,
                                   HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        preencherModeloBase(model, session);
        model.addAttribute("produtores", adminService.listarProdutores(pesquisa));
        model.addAttribute("pesquisa", pesquisa);
        return "admin/gerir-produtores";
    }

    @DeleteMapping("/produtores/{id}")
    public String removerProdutor(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        adminService.removerProdutor(id, session);
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
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        try {
            var p = adminService.criarProdutor(nome, apelido, email, telefone, nuit, senha, session);
            ra.addFlashAttribute("sucesso", "Produtor " + p.getNomeCompleto() + " criado com sucesso.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/produtores";
    }

    /** Valida manualmente a conta de um produtor (sem precisar do link do e-mail). */
    @PostMapping("/produtores/{id}/validar")
    public String validarProdutor(@PathVariable Long id,
                                  @RequestParam(required = false) String voltar,
                                  HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        try {
            var p = adminService.validarProdutor(id, session);
            ra.addFlashAttribute("sucesso", "Conta de " + p.getNomeCompleto() + " validada com sucesso.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "detalhe".equals(voltar) ? "redirect:/admin/produtores/" + id : "redirect:/admin/produtores";
    }

    // ════════ COMPRADORES ════════

    @GetMapping("/compradores")
    public String listarCompradores(@RequestParam(required = false) String pesquisa,
                                    HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        preencherModeloBase(model, session);
        model.addAttribute("compradores", adminService.listarCompradores(pesquisa));
        model.addAttribute("pesquisa", pesquisa);
        return "admin/gerir-compradores";
    }

    @DeleteMapping("/compradores/{id}")
    public String removerComprador(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        adminService.removerComprador(id, session);
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
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        try {
            var c = adminService.criarComprador(nome, apelido, email, telefone, nuit, senha, session);
            ra.addFlashAttribute("sucesso", "Comprador " + c.getNomeCompleto() + " criado com sucesso.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/compradores";
    }

    /** Valida manualmente a conta de um comprador (sem precisar do link do e-mail). */
    @PostMapping("/compradores/{id}/validar")
    public String validarComprador(@PathVariable Long id,
                                   @RequestParam(required = false) String voltar,
                                   HttpSession session, RedirectAttributes ra) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        try {
            var c = adminService.validarComprador(id, session);
            ra.addFlashAttribute("sucesso", "Conta de " + c.getNomeCompleto() + " validada com sucesso.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "detalhe".equals(voltar) ? "redirect:/admin/compradores/" + id : "redirect:/admin/compradores";
    }

    // ════════ DETALHES COMPRADOR ════════

    @GetMapping("/compradores/{id}")
    public String verComprador(@PathVariable Long id, HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        var d = adminService.detalheComprador(id);
        preencherModeloBase(model, session);
        model.addAttribute("comprador",    d.comprador());
        model.addAttribute("pedidos",      d.pedidos());
        model.addAttribute("totalPedidos", d.totalPedidos());
        model.addAttribute("aceites",      d.aceites());
        model.addAttribute("pendentes",    d.pendentes());
        model.addAttribute("rejeitados",   d.rejeitados());
        return "admin/detalhe-comprador";
    }

    // ════════ DETALHES PRODUTOR ════════

    @GetMapping("/produtores/{id}")
    public String verProdutor(@PathVariable Long id, HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        var d = adminService.detalheProdutor(id);
        preencherModeloBase(model, session);
        model.addAttribute("produtor",             d.produtor());
        model.addAttribute("ofertas",              d.ofertas());
        model.addAttribute("totalOfertas",         d.totalOfertas());
        model.addAttribute("ofertasAtivas",        d.ofertasAtivas());
        model.addAttribute("ofertasExpiradas",     d.ofertasExpiradas());
        model.addAttribute("ofertasInativas",      d.ofertasInativas());
        model.addAttribute("pedidosProdutor",      d.pedidosProdutor());
        model.addAttribute("totalPedidosProdutor", d.totalPedidosProdutor());
        model.addAttribute("pedAceites",           d.pedAceites());
        model.addAttribute("pedPendentes",         d.pedPendentes());
        model.addAttribute("pedRejeitados",        d.pedRejeitados());
        return "admin/detalhe-produtor";
    }

    // ════════ VENDAS ════════

    @GetMapping("/vendas")
    public String listarVendas(@RequestParam(required = false) String estado,
                               HttpSession session, Model model) {
        if (!adminAuthService.isAdmin(session)) return "redirect:/admin/login";
        preencherModeloBase(model, session);
        model.addAttribute("pedidos", adminService.listarVendas(estado));
        model.addAttribute("estadoFiltro", estado);
        return "admin/ver-vendas";
    }
}
