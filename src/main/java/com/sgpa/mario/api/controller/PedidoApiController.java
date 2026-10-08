package com.sgpa.mario.api.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgpa.mario.api.config.UsuarioAtual;
import com.sgpa.mario.api.dto.PageResponse;
import com.sgpa.mario.api.dto.PedidoDtos.AceitarPedidoRequest;
import com.sgpa.mario.api.dto.PedidoDtos.PagamentoRequest;
import com.sgpa.mario.api.dto.PedidoDtos.PedidoCreateRequest;
import com.sgpa.mario.api.dto.PedidoDtos.PedidoResponse;
import com.sgpa.mario.api.dto.PedidoDtos.RejeitarPedidoRequest;
import com.sgpa.mario.api.service.ApiPedidoService;
import com.sgpa.mario.web.domain.Pedido.EstadoPedido;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos e pagamentos")
@SecurityRequirement(name = "bearerAuth")
public class PedidoApiController {

    private final ApiPedidoService pedidoService;

    public PedidoApiController(ApiPedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COMPRADOR')")
    @Operation(summary = "Cria um pedido de compra (Comprador)")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoCreateRequest req) {
        PedidoResponse criado = pedidoService.criar(UsuarioAtual.obter().id(), req);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + criado.id())).body(criado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPRADOR','PRODUTOR')")
    @Operation(summary = "Lista os meus pedidos (Comprador) ou pedidos recebidos (Produtor)")
    public PageResponse<PedidoResponse> listar(@RequestParam(required = false) EstadoPedido estado,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int tamanho) {
        return PageResponse.de(pedidoService.listar(UsuarioAtual.obter(), estado),
            Paginacao.pagina(pagina), Paginacao.tamanho(tamanho));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um pedido (comprador, produtor da oferta ou admin)")
    public PedidoResponse obter(@PathVariable Long id) {
        return pedidoService.obter(UsuarioAtual.obter(), id);
    }

    @PatchMapping("/{id}/aceitar")
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Aceita um pedido pendente e indica o número para receber o pagamento (Produtor)")
    public PedidoResponse aceitar(@PathVariable Long id, @Valid @RequestBody AceitarPedidoRequest req) {
        return pedidoService.aceitar(UsuarioAtual.obter().id(), id, req.numeroPagamento());
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Rejeita um pedido indicando o motivo (Produtor)")
    public PedidoResponse rejeitar(@PathVariable Long id, @Valid @RequestBody RejeitarPedidoRequest req) {
        return pedidoService.rejeitar(UsuarioAtual.obter().id(), id, req.motivo());
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('COMPRADOR')")
    @Operation(summary = "Cancela um pedido ainda pendente (Comprador)")
    public PedidoResponse cancelar(@PathVariable Long id) {
        return pedidoService.cancelar(UsuarioAtual.obter().id(), id);
    }

    @PostMapping("/{id}/pagamento")
    @PreAuthorize("hasRole('COMPRADOR')")
    @Operation(summary = "Regista o pagamento de um pedido aceite (Comprador)")
    public PedidoResponse pagar(@PathVariable Long id, @Valid @RequestBody PagamentoRequest req) {
        return pedidoService.pagar(UsuarioAtual.obter().id(), id, req);
    }
}
