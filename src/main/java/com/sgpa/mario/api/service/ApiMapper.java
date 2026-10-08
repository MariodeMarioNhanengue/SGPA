package com.example.demo.api.service;

import java.math.BigDecimal;

import com.example.demo.api.dto.CatalogoDtos.BairroResponse;
import com.example.demo.api.dto.CatalogoDtos.DistritoResponse;
import com.example.demo.api.dto.CatalogoDtos.ProvinciaResponse;
import com.example.demo.api.dto.OfertaDtos.LocalizacaoResumo;
import com.example.demo.api.dto.OfertaDtos.OfertaResponse;
import com.example.demo.api.dto.OfertaDtos.ProdutorResumo;
import com.example.demo.api.dto.PedidoDtos.CompradorResumo;
import com.example.demo.api.dto.PedidoDtos.OfertaResumo;
import com.example.demo.api.dto.PedidoDtos.PedidoResponse;
import com.example.demo.api.dto.PedidoDtos.TransacaoResponse;
import com.example.demo.api.dto.UsuarioDtos.UsuarioResponse;
import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Distrito;
import com.example.demo.web.domain.Oferta;
import com.example.demo.web.domain.Pedido;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.domain.Provincia;
import com.example.demo.web.domain.Transacao;

/** Converte entidades JPA em DTOs. Nunca expõe senhas nem relações internas. */
public final class ApiMapper {

    private ApiMapper() {}

    // ─── Utilizadores ────────────────────────────────────────────────────

    public static UsuarioResponse produtor(Produtor p, String outroPerfil) {
        return new UsuarioResponse("PRODUTOR", p.getId(), p.getNome(), p.getApelido(), p.getEmail(),
            p.getTelefone(), p.getNuit(), p.getFotoPerfil(), p.getEmailVerificado(), p.isContaAtiva(), null,
            null, null, null, outroPerfil, p.getUltimoAcesso());
    }

    public static UsuarioResponse comprador(Comprador c, String outroPerfil) {
        Bairro b = c.getBairros();
        Distrito d = b == null ? null : b.getDistrito();
        Provincia pv = d == null ? null : d.getProvincia();
        return new UsuarioResponse("COMPRADOR", c.getId(), c.getNome(), c.getApelido(), c.getEmail(),
            c.getTelefone(), c.getNuit(), c.getFotoPerfil(), c.getEmailVerificado(), c.isContaAtiva(), null,
            pv == null ? null : pv.getNome(), d == null ? null : d.getNome(), b == null ? null : b.getNome(),
            outroPerfil, c.getUltimoAcesso());
    }

    public static UsuarioResponse admin(Admin a) {
        return new UsuarioResponse("ADMIN", a.getId(), a.getNome(), a.getApelido(), a.getEmail(),
            null, null, a.getFotoPerfil(), Boolean.TRUE, Boolean.TRUE, a.isSuperAdmin(),
            null, null, null, null, a.getUltimoAcesso());
    }

    // ─── Ofertas ─────────────────────────────────────────────────────────

    public static OfertaResponse oferta(Oferta o) {
        Produtor p = o.getProdutor();
        ProdutorResumo produtor = p == null ? null : new ProdutorResumo(p.getId(), p.getNomeCompleto());
        Bairro b = o.getBairro();
        Distrito d = b == null ? null : b.getDistrito();
        Provincia pv = d == null ? null : d.getProvincia();
        LocalizacaoResumo loc = b == null ? null : new LocalizacaoResumo(
            pv == null ? null : pv.getId(), pv == null ? null : pv.getNome(),
            d == null ? null : d.getId(), d == null ? null : d.getNome(),
            b.getId(), b.getNome());
        return new OfertaResponse(o.getId(), o.getNomeProduto(), o.getCategoria(), o.getUnidade(),
            o.getQuantidadeTotal(), o.getQuantidadeRestante(), o.getPreco(), o.getDescricao(),
            o.getImagemPath(), o.getEstado(), o.getDataPublicacao(), o.getDataValidade(), produtor, loc);
    }

    // ─── Pedidos ─────────────────────────────────────────────────────────

    public static BigDecimal total(Pedido p) {
        if (p.getQuantidade() != null && p.getOferta() != null && p.getOferta().getPreco() != null) {
            return p.getQuantidade().multiply(p.getOferta().getPreco());
        }
        return BigDecimal.ZERO;
    }

    public static TransacaoResponse transacao(Transacao t) {
        if (t == null) return null;
        return new TransacaoResponse(t.getId(), t.getMetodoPagamento(), t.getOperadora(), t.getReferencia(),
            t.getValorTotal(), t.getEstado(), t.getDataTransacao(), t.getDataConfirmacao());
    }

    public static PedidoResponse pedido(Pedido p, Transacao t) {
        Oferta o = p.getOferta();
        OfertaResumo oferta = null;
        if (o != null) {
            Produtor prod = o.getProdutor();
            oferta = new OfertaResumo(o.getId(), o.getNomeProduto(), o.getCategoria(), o.getUnidade(),
                o.getPreco(), o.getImagemPath(),
                prod == null ? null : prod.getId(), prod == null ? null : prod.getNomeCompleto());
        }
        Comprador c = p.getComprador();
        CompradorResumo comprador = c == null ? null : new CompradorResumo(c.getId(), c.getNomeCompleto(), c.getTelefone());
        return new PedidoResponse(p.getId(), p.getEstado(), p.getQuantidade(), total(p), p.getDataPedido(),
            p.getDataResposta(), p.getMotivoRejeicao(), p.getMotivoCancelamento(), p.getNumeroPagamentoProdutor(),
            oferta, comprador, transacao(t));
    }

    // ─── Localização ─────────────────────────────────────────────────────

    public static ProvinciaResponse provincia(Provincia p) {
        return new ProvinciaResponse(p.getId(), p.getNome());
    }

    public static DistritoResponse distrito(Distrito d) {
        return new DistritoResponse(d.getId(), d.getNome(), d.getProvincia() == null ? null : d.getProvincia().getId());
    }

    public static BairroResponse bairro(Bairro b) {
        return new BairroResponse(b.getId(), b.getNome(), b.getDistrito() == null ? null : b.getDistrito().getId());
    }
}
