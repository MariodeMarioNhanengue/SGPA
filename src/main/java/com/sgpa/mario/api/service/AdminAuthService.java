package com.sgpa.mario.api.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sgpa.mario.web.domain.Admin;
import com.sgpa.mario.web.domain.AdminLog;
import com.sgpa.mario.api.repository.AdminLogRepository;
import com.sgpa.mario.api.repository.AdminRepository;
import jakarta.servlet.http.HttpSession;

/**
 * Regras de negócio de autenticação e permissões da área de administração.
 * Concentra todas as verificações de "quem pode fazer o quê" (isAdmin,
 * isSuperAdmin) e o registo de auditoria (log) das acções do administrador.
 */
@Service
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminLogRepository adminLogRepository;
    private final PasswordEncoderService passwordEncoderService;
    private final LoginAttemptService loginAttemptService;

    public AdminAuthService(AdminRepository adminRepository, AdminLogRepository adminLogRepository,
                            PasswordEncoderService passwordEncoderService,
                            LoginAttemptService loginAttemptService) {
        this.adminRepository = adminRepository;
        this.adminLogRepository = adminLogRepository;
        this.passwordEncoderService = passwordEncoderService;
        this.loginAttemptService = loginAttemptService;
    }

    /** Resultado de uma tentativa de login de administrador. */
    public record ResultadoLoginAdmin(Admin admin, boolean bloqueado, long minutosRestantes) {
        public boolean sucesso() { return admin != null; }
        public static ResultadoLoginAdmin ok(Admin a) { return new ResultadoLoginAdmin(a, false, 0); }
        public static ResultadoLoginAdmin falha() { return new ResultadoLoginAdmin(null, false, 0); }
        public static ResultadoLoginAdmin bloqueado(long minutos) { return new ResultadoLoginAdmin(null, true, minutos); }
    }

    public ResultadoLoginAdmin autenticar(String email, String senha, HttpSession session) {
        if (loginAttemptService.estaBloqueado("admin:" + email)) {
            return ResultadoLoginAdmin.bloqueado(loginAttemptService.minutosRestantes("admin:" + email));
        }

        Optional<Admin> opt = adminRepository.findByEmail(email);
        if (opt.isPresent()) {
            Admin a = opt.get();
            if (passwordEncoderService.matches(senha, a.getSenha())) {
                loginAttemptService.registarSucesso("admin:" + email);
                if (!passwordEncoderService.isHash(a.getSenha())) {
                    a.setSenha(passwordEncoderService.encode(senha));
                }
                a.setUltimoAcesso(LocalDateTime.now());
                adminRepository.save(a);
                iniciarSessao(session, a);
                registarLog(session, "LOGIN", "Sessão iniciada.");
                return ResultadoLoginAdmin.ok(a);
            }
        }
        loginAttemptService.registarFalha("admin:" + email);
        return ResultadoLoginAdmin.falha();
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private void iniciarSessao(HttpSession session, Admin admin) {
        session.setAttribute("loggedInType", "ADMIN");
        session.setAttribute("loggedInId", admin.getId());
        session.setAttribute("loggedInNome", admin.getNomeCompleto());
        session.setAttribute("isSuperAdmin", admin.isSuperAdmin());
        session.setAttribute("adminFoto", admin.getFotoPerfil());
    }

    /** Actualiza os dados de sessão após uma alteração de perfil do próprio admin. */
    public void atualizarSessao(HttpSession session, Admin admin) {
        session.setAttribute("loggedInNome", admin.getNomeCompleto());
        session.setAttribute("adminFoto", admin.getFotoPerfil());
    }

    // ─── Permissões ──────────────────────────────────────────────────────

    public boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("loggedInType"));
    }

    public boolean isSuperAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isSuperAdmin"));
    }

    public Long getAdminId(HttpSession session) {
        Object id = session.getAttribute("loggedInId");
        return id != null ? ((Number) id).longValue() : null;
    }

    // ─── Auditoria ───────────────────────────────────────────────────────

    /** Regista uma acção do administrador no log. */
    public void registarLog(HttpSession session, String tipoAcao, String descricao,
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

    /** Sobrecarga para acções sem afectado (login, editar perfil). */
    public void registarLog(HttpSession session, String tipoAcao, String descricao) {
        registarLog(session, tipoAcao, descricao, null, null);
    }
}
