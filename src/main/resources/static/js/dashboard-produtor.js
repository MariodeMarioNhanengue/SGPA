/* ================================================
   dashboard-produtor.js
   Integrates with Spring Boot REST API
================================================ */
$(function () {

  const produtorId   = sessionStorage.getItem('produtorId')   || 1;
  const produtorNome = sessionStorage.getItem('produtorNome') || 'Produtor';
  $('#sidebarNome').text(produtorNome);

  /* ---- Load stats on init ---- */
  loadStats();
  loadUltimasOfertas();

  function loadStats() {
    $.get(`/api/produtores/${produtorId}/stats`)
      .done(function (data) {
        $('#totalOfertas').text(data.ofertas     || 0);
        $('#totalPedidos').text(data.pedidos     || 0);
        $('#pedidosPendentes').text(data.pendentes || 0);
        $('#pedidosAceites').text(data.vendas    || 0);
      })
      .fail(function () {
        // Demo values
        $('#totalOfertas').text('3');
        $('#totalPedidos').text('7');
        $('#pedidosPendentes').text('2');
        $('#pedidosAceites').text('5');
      });
  }

  function loadUltimasOfertas() {
    $.get(`/api/produtores/${produtorId}/ofertas?limit=3`)
      .done(function (data) {
        renderUltimasOfertas(data);
      })
      .fail(function () {
        $('#listaUltimasOfertas').html('<p class="text-muted small text-center py-2">Sem dados disponíveis.</p>');
      });
  }

  function renderUltimasOfertas(lista) {
    if (!lista || lista.length === 0) return;
    let html = '';
    lista.forEach(function (o) {
      html += `
        <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
          <div>
            <div class="fw-semibold small">${o.nomeProduto}</div>
            <div class="text-muted" style="font-size:.78rem">${o.quantidadeTotal} ${o.unidade}</div>
          </div>
          <span class="badge ${o.estado === 'ativo' ? 'bg-success' : 'bg-secondary'}">${o.estado}</span>
        </div>`;
    });
    $('#listaUltimasOfertas').html(html);
  }

  /* ---- Tab: Minhas Ofertas — load on tab click ---- */
  $(document).on('click', '.sidebar-link[data-tab="minhas-ofertas"]', function () {
    loadMinhasOfertas();
  });

  function loadMinhasOfertas() {
    $.get(`/api/produtores/${produtorId}/ofertas`)
      .done(function (data) { renderTabelaOfertas(data); })
      .fail(function ()     { /* sem dados */ });
  }

  function renderTabelaOfertas(lista) {
    if (!lista || lista.length === 0) return;
    let html = '';
    lista.forEach(function (o, i) {
      html += `
        <tr>
          <td>${i + 1}</td>
          <td><strong>${o.nomeProduto}</strong><br><small class="text-muted">${o.categoria}</small></td>
          <td>${o.quantidadeRestante}/${o.quantidadeTotal} ${o.unidade}</td>
          <td>MT ${parseFloat(o.preco).toFixed(2)}</td>
          <td>${o.dataValidade || '—'}</td>
          <td><span class="badge ${o.estado === 'ativo' ? 'bg-success' : 'bg-secondary'}">${o.estado}</span></td>
          <td>
            <button class="btn btn-sm btn-outline-primary btn-editar" data-id="${o.idOferta}"
                    data-preco="${o.preco}" data-qty="${o.quantidadeRestante}"
                    data-bs-toggle="modal" data-bs-target="#modalEditar">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-sm btn-outline-danger btn-remover" data-id="${o.idOferta}">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        </tr>`;
    });
    $('#tbodyOfertas').html(html);
  }

  /* ---- Tab: Pedidos — load on tab click ---- */
  $(document).on('click', '.sidebar-link[data-tab="pedidos"]', function () {
    loadPedidos();
  });

  function loadPedidos(estado = '') {
    const url = `/api/produtores/${produtorId}/pedidos${estado ? '?estado=' + estado : ''}`;
    $.get(url)
      .done(function (data) { renderTabelaPedidos(data); })
      .fail(function ()     { /* sem dados */ });
  }

  $('#filtroPedidos').on('change', function () {
    loadPedidos($(this).val());
  });

  function renderTabelaPedidos(lista) {
    if (!lista || lista.length === 0) return;
    let html = '';
    lista.forEach(function (p, i) {
      const badgeClass = p.estado === 'aceite' ? 'bg-success'
                       : p.estado === 'rejeitado' ? 'bg-danger' : 'bg-warning text-dark';
      html += `
        <tr>
          <td>${i + 1}</td>
          <td>${p.nomeComprador}</td>
          <td>${p.nomeProduto}</td>
          <td>${p.dataPedido}</td>
          <td><span class="badge ${badgeClass}">${p.estado}</span></td>
          <td>
            ${p.estado === 'pendente' ? `
              <button class="btn btn-sm btn-success btn-aceitar" data-id="${p.idPedido}">
                <i class="bi bi-check-lg"></i> Aceitar
              </button>
              <button class="btn btn-sm btn-outline-danger btn-rejeitar" data-id="${p.idPedido}">
                <i class="bi bi-x-lg"></i>
              </button>` : '—'}
          </td>
        </tr>`;
    });
    $('#tbodyPedidos').html(html);
  }

  /* ---- Aceitar / Rejeitar pedido ---- */
  $(document).on('click', '.btn-aceitar', function () {
    const id = $(this).data('id');
    updatePedido(id, 'aceite');
  });
  $(document).on('click', '.btn-rejeitar', function () {
    const id = $(this).data('id');
    updatePedido(id, 'rejeitado');
  });

  function updatePedido(id, estado) {
    $.ajax({
      url: `/api/pedidos/${id}/estado`,
      method: 'PATCH',
      contentType: 'application/json',
      data: JSON.stringify({ estado })
    })
    .done(function () {
      showToast(`Pedido ${estado} com sucesso!`, 'success');
      loadPedidos();
    })
    .fail(function () {
      showToast('Erro ao actualizar pedido.', 'error');
    });
  }

  /* ---- Publicar Oferta ---- */
  $('#formOferta').on('submit', function (e) {
    e.preventDefault();
    if (!this.checkValidity()) { $(this).addClass('was-validated'); return; }

    const payload = {
      produtorId:       parseInt(produtorId),
      categoriaId:      1,  // substituir pelo ID real
      nomeProduto:      $('#nomeProduto').val().trim(),
      descricao:        $('#descricaoProduto').val().trim(),
      quantidadeTotal:  parseFloat($('#quantidadeTotal').val()),
      unidade:          $('#unidade').val(),
      preco:            parseFloat($('#preco').val()),
      dataPublicacao:   $('#dataPublicacao').val(),
      dataValidade:     $('#dataValidade').val(),
      bairro:           $('#bairroOferta').val().trim(),
      estado:           $('#estadoOferta').val()
    };

    $.ajax({
      url: '/api/ofertas',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload)
    })
    .done(function () {
      showToast('Oferta publicada com sucesso!', 'success');
      $('#formOferta')[0].reset();
      $('#formOferta').removeClass('was-validated');
      loadStats();
    })
    .fail(function () {
      showToast('Erro ao publicar oferta.', 'error');
    });
  });

  /* ---- Modal Editar: preencher campos ---- */
  $(document).on('click', '.btn-editar', function () {
    $('#editPreco').val($(this).data('preco'));
    $('#editQuantidade').val($(this).data('qty'));
    $('#btnSalvarEdicao').data('id', $(this).data('id'));
  });

  $('#btnSalvarEdicao').on('click', function () {
    const id = $(this).data('id');
    const payload = {
      preco:             parseFloat($('#editPreco').val()),
      quantidadeRestante: parseFloat($('#editQuantidade').val())
    };
    $.ajax({
      url: `/api/ofertas/${id}`,
      method: 'PATCH',
      contentType: 'application/json',
      data: JSON.stringify(payload)
    })
    .done(function () {
      bootstrap.Modal.getInstance(document.getElementById('modalEditar')).hide();
      showToast('Oferta actualizada!', 'success');
      loadMinhasOfertas();
    })
    .fail(function () {
      showToast('Erro ao actualizar oferta.', 'error');
    });
  });

  /* ---- Pesquisa na tabela de ofertas ---- */
  $('#searchOfertas').on('input', function () {
    const q = $(this).val().toLowerCase();
    $('#tbodyOfertas tr').each(function () {
      $(this).toggle($(this).text().toLowerCase().includes(q));
    });
  });

  /* ---- Tab: Histórico ---- */
  $(document).on('click', '.sidebar-link[data-tab="historico"]', function () {
    $.get(`/api/produtores/${produtorId}/historico`)
      .done(function (data) {
        if (!data || data.length === 0) return;
        let html = '';
        data.forEach(function (v, i) {
          html += `
            <tr>
              <td>${i + 1}</td>
              <td>${v.nomeProduto}</td>
              <td>${v.nomeComprador}</td>
              <td>${v.quantidade} ${v.unidade}</td>
              <td>MT ${parseFloat(v.total).toFixed(2)}</td>
              <td>${v.data}</td>
              <td><span class="badge bg-success">${v.estado}</span></td>
            </tr>`;
        });
        $('#tbodyHistorico').html(html);
      });
  });

});
