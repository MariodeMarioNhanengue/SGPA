package com.example.demo.api.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.api.config.UsuarioAtual;
import com.example.demo.api.dto.UsuarioDtos.AlterarSenhaRequest;
import com.example.demo.api.dto.UsuarioDtos.AtualizarPerfilRequest;
import com.example.demo.api.dto.UsuarioDtos.RegistoCompradorRequest;
import com.example.demo.api.dto.UsuarioDtos.RegistoProdutorRequest;
import com.example.demo.api.dto.UsuarioDtos.UsuarioResponse;
import com.example.demo.api.exception.ApiException;
import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.api.repository.AdminRepository;
import com.example.demo.api.repository.CompradorRepository;
import com.example.demo.api.repository.ProdutorRepository;
import com.example.demo.api.service.ArmazenamentoService;
import com.example.demo.api.service.CompradorService;
import com.example.demo.api.service.LocalizacaoService;
import com.example.demo.api.service.PasswordEncoderService;
import com.example.demo.api.service.ProdutorService;
import com.example.demo.api.service.exception.RecursoNaoEncontradoException;
import com.example.demo.api.service.exception.RegraNegocioException;

/** Registo, perfil, foto e senha de Produtores, Compradores e Administradores. */
@Service
public class ApiUsuarioService {

    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final AdminRepository adminRepository;
    private final ProdutorService produtorService;
    private final CompradorService compradorService;
    private final LocalizacaoService localizacaoService;
    private final ArmazenamentoService armazenamentoService;
    private final PasswordEncoderService passwords;

    public ApiUsuarioService(ProdutorRepository produtorRepository, CompradorRepository compradorRepository,
                             AdminRepository adminRepository, ProdutorService produtorService,
                             CompradorService compradorService, LocalizacaoService localizacaoService,
                             ArmazenamentoService armazenamentoService, PasswordEncoderService passwords) {
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.adminRepository = adminRepository;
        this.produtorService = produtorService;
        this.compradorService = compradorService;
        this.localizacaoService = localizacaoService;
        this.armazenamentoService = armazenamentoService;
        this.passwords = passwords;
    }

    // ─── Mapeamento com "outro perfil" ───────────────────────────────────

    public UsuarioResponse paraProdutor(Produtor p) {
        boolean temComprador = p.getEmail() != null && compradorRepository.findByEmail(p.getEmail()).isPresent();
        return ApiMapper.produtor(p, temComprador ? UsuarioAtual.COMPRADOR : null);
    }

    public UsuarioResponse paraComprador(Comprador c) {
        boolean temProdutor = c.getEmail() != null && produtorRepository.findByEmail(c.getEmail()).isPresent();
        return ApiMapper.comprador(c, temProdutor ? UsuarioAtual.PRODUTOR : null);
    }

    // ─── Leitura ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UsuarioResponse obter(UsuarioAtual.Dados u) {
        return switch (u.tipo()) {
            case UsuarioAtual.PRODUTOR -> paraProdutor(produtor(u.id()));
            case UsuarioAtual.COMPRADOR -> paraComprador(comprador(u.id()));
            case UsuarioAtual.ADMIN -> ApiMapper.admin(admin(u.id()));
            default -> throw tipoDesconhecido();
        };
    }

    @Transactional(readOnly = true)
    public String emailDe(UsuarioAtual.Dados u) {
        return switch (u.tipo()) {
            case UsuarioAtual.PRODUTOR -> produtor(u.id()).getEmail();
            case UsuarioAtual.COMPRADOR -> comprador(u.id()).getEmail();
            case UsuarioAtual.ADMIN -> admin(u.id()).getEmail();
            default -> throw tipoDesconhecido();
        };
    }

    // ─── Registo ─────────────────────────────────────────────────────────

    public UsuarioResponse registarComprador(RegistoCompradorRequest r) {
        Comprador c = new Comprador();
        c.setNome(r.nome().trim());
        c.setApelido(r.apelido().trim());
        c.setTelefone(r.telefone().trim());
        c.setEmail(r.email().trim());
        c.setNuit(vazioParaNull(r.nuit()));
        c.setSenha(r.senha());
        // o serviço existente valida duplicados, guarda o hash BCrypt e envia o e-mail de confirmação
        Comprador salvo = compradorService.cadastrar(c, r.provincia(), r.distrito(), r.bairro(), null);
        return paraComprador(salvo);
    }

    public UsuarioResponse registarProdutor(RegistoProdutorRequest r) {
        Produtor p = new Produtor();
        p.setNome(r.nome().trim());
        p.setApelido(r.apelido().trim());
        p.setTelefone(r.telefone().trim());
        p.setEmail(r.email().trim());
        p.setNuit(vazioParaNull(r.nuit()));
        p.setSenha(r.senha());
        Produtor salvo = produtorService.cadastrar(p, null);
        return paraProdutor(salvo);
    }

    // ─── Perfil ──────────────────────────────────────────────────────────

    @Transactional
    public UsuarioResponse atualizar(UsuarioAtual.Dados u, AtualizarPerfilRequest r) {
        return switch (u.tipo()) {
            case UsuarioAtual.PRODUTOR -> atualizarProdutor(u.id(), r);
            case UsuarioAtual.COMPRADOR -> atualizarComprador(u.id(), r);
            case UsuarioAtual.ADMIN -> atualizarAdmin(u.id(), r);
            default -> throw tipoDesconhecido();
        };
    }

    private UsuarioResponse atualizarProdutor(Long id, AtualizarPerfilRequest r) {
        Produtor p = produtor(id);
        String email = r.email().trim();
        String nuit = vazioParaNull(r.nuit());
        produtorRepository.findByEmail(email)
            .filter(o -> !o.getId().equals(id))
            .ifPresent(o -> { throw new RegraNegocioException("E-mail já registado por outro produtor."); });
        if (nuit != null && !nuit.equals(p.getNuit())) {
            produtorRepository.findByNuit(nuit)
                .filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw new RegraNegocioException("Este NUIT já está registado por outro produtor."); });
        }
        p.setNome(r.nome().trim());
        p.setApelido(r.apelido().trim());
        p.setEmail(email);
        p.setNuit(nuit);
        if (r.telefone() != null && !r.telefone().isBlank()) p.setTelefone(r.telefone().trim());
        produtorRepository.save(p);
        return paraProdutor(p);
    }

    private UsuarioResponse atualizarComprador(Long id, AtualizarPerfilRequest r) {
        Comprador c = comprador(id);
        String email = r.email().trim();
        String nuit = vazioParaNull(r.nuit());
        compradorRepository.findByEmail(email)
            .filter(o -> !o.getId().equals(id))
            .ifPresent(o -> { throw new RegraNegocioException("E-mail já registado por outro comprador."); });
        if (nuit != null && !nuit.equals(c.getNuit())) {
            compradorRepository.findByNuit(nuit)
                .filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw new RegraNegocioException("Este NUIT já está registado por outro comprador."); });
        }
        c.setNome(r.nome().trim());
        c.setApelido(r.apelido().trim());
        c.setEmail(email);
        c.setNuit(nuit);
        if (r.telefone() != null && !r.telefone().isBlank()) c.setTelefone(r.telefone().trim());
        if (r.provincia() != null && !r.provincia().isBlank()) {
            Bairro b = localizacaoService.resolverOuCriarBairro(r.provincia(), r.distrito(), r.bairro());
            if (b != null) c.setBairros(b);
        }
        compradorRepository.save(c);
        return paraComprador(c);
    }

    private UsuarioResponse atualizarAdmin(Long id, AtualizarPerfilRequest r) {
        Admin a = admin(id);
        String email = r.email().trim();
        adminRepository.findByEmail(email)
            .filter(o -> !o.getId().equals(id))
            .ifPresent(o -> { throw new RegraNegocioException("E-mail já em uso por outro administrador."); });
        a.setNome(r.nome().trim());
        a.setApelido(r.apelido().trim());
        a.setEmail(email);
        adminRepository.save(a);
        return ApiMapper.admin(a);
    }

    @Transactional
    public UsuarioResponse atualizarFoto(UsuarioAtual.Dados u, MultipartFile foto) {
        ImagemUtil.validar(foto);
        String url = armazenamentoService.guardarFotoPerfil(foto, u.tipo().toLowerCase(Locale.ROOT), u.id());
        if (url == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_GUARDAR_FICHEIRO",
                "Não foi possível guardar a imagem.");
        }
        switch (u.tipo()) {
            case UsuarioAtual.PRODUTOR -> { Produtor p = produtor(u.id()); p.setFotoPerfil(url); produtorRepository.save(p); }
            case UsuarioAtual.COMPRADOR -> { Comprador c = comprador(u.id()); c.setFotoPerfil(url); compradorRepository.save(c); }
            case UsuarioAtual.ADMIN -> { Admin a = admin(u.id()); a.setFotoPerfil(url); adminRepository.save(a); }
            default -> throw tipoDesconhecido();
        }
        return obter(u);
    }

    @Transactional
    public void alterarSenha(UsuarioAtual.Dados u, AlterarSenhaRequest r) {
        switch (u.tipo()) {
            case UsuarioAtual.PRODUTOR -> {
                Produtor p = produtor(u.id());
                verificarSenhaAtual(r.senhaAtual(), p.getSenha());
                p.setSenha(passwords.encode(r.novaSenha()));
                produtorRepository.save(p);
            }
            case UsuarioAtual.COMPRADOR -> {
                Comprador c = comprador(u.id());
                verificarSenhaAtual(r.senhaAtual(), c.getSenha());
                c.setSenha(passwords.encode(r.novaSenha()));
                compradorRepository.save(c);
            }
            case UsuarioAtual.ADMIN -> {
                Admin a = admin(u.id());
                verificarSenhaAtual(r.senhaAtual(), a.getSenha());
                a.setSenha(passwords.encode(r.novaSenha()));
                adminRepository.save(a);
            }
            default -> throw tipoDesconhecido();
        }
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private void verificarSenhaAtual(String fornecida, String guardada) {
        if (!passwords.matches(fornecida, guardada)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SENHA_ATUAL_INCORRECTA", "A senha actual está incorrecta.");
        }
    }

    private Produtor produtor(Long id) {
        return produtorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
    }

    private Comprador comprador(Long id) {
        return compradorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));
    }

    private Admin admin(Long id) {
        return adminRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador não encontrado."));
    }

    private ApiException tipoDesconhecido() {
        return new ApiException(HttpStatus.FORBIDDEN, "TIPO_DESCONHECIDO", "Tipo de utilizador desconhecido.");
    }

    private static String vazioParaNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
