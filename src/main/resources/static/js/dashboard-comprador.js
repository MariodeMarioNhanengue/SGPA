/* ================================================
   dashboard-comprador.js
   Integrates with Spring Boot REST API
================================================ */
$(function () {

  const compradorId   = sessionStorage.getItem('compradorId')   || 1;
  const compradorNome = sessionStorage.getItem('compradorNome') || 'Comprador';
  $('#sidebarNome').text(compradorNome);

  /* ---- Load produtos on init ---- */
  loadOfertas();

  function loadOfertas(params = {}) {
    const qs = $.param(params);
    $.get('/api/ofertas?' + qs)
      .done(function (data) {
        renderOfertas(data);
      })
      .fail(function () {
        // Manter os placeholders HTML se API não responder
        $('#resultCount').text('A mostrar produtos de demonstração.');
      });
  }

  function renderOfertas(lista) {
    if (!lista || lista.length === 0) {
      $('#produtosGrid').html('<div class="col-12 text-center text-muted py-5"><i class="bi bi-search fs-2 d-block mb-2"></i>Nenhuma oferta encontrada.</div>');
      $('#resultCount').text('0 ofertas encontradas.');
      return;
    }

    $('#resultCount').text(`${lista.length} oferta(s) encontrada(s).`);
    let html = '';
    const icons  = ['bi-basket2-fill','bi-apple','bi-flower1','bi-egg-fill','bi-tree'];
    const themes = ['','alt','green','','alt'];

    lista.forEach(function (o, i) {
      const icon  = icons[i % icons.length];
      const theme = themes[i % themes.length];
      const badge = o.quantidadeRestante < (o.quantidadeTotal * 0.2) ? 'warning">A esgotar' : '">Activo';
      html += `
        <div class="col-sm-6 col-xl-4" data-oferta-id="${o.idOferta}">
          <div class="product-card" data-oferta='${JSON.stringify(o)}'>
            <div class="product-card-badge ${badge}</div>
            <div class="product-card-icon ${theme}"><i class="bi ${icon}"></i></div>
            <div class="product-card-body p-3">
              <div class="d-flex justify-content-between align-items-start mb-1">
                <h6 class="product-name mb-0">${o.nomeProduto}</h6>
                <span class="product-category-tag">${o.categoriaNome || '—'}</span>
              </div>
              <p class="product-desc text-muted small mb-2">${o.descricao || 'Sem descrição.'}</p>
              <div class="product-meta d-flex gap-3 mb-3">
                <span><i class="bi bi-stack me-1 text-accent"></i>${o.quantidadeRestante} ${o.unidade}</span>
                <span><i class="bi bi-geo-alt me-1 text-accent"></i>${o.provincia || o.bairro || '—'}</span>
              </div>
              <div class="d-flex justify-content-between align-items-center">
                <div class="product-price">MT ${parseFloat(o.preco || 0).toFixed(2)}<span class="small text-muted">/${o.unidade}</span></div>
                <button class="btn btn-accent-blue btn-sm btn-ver-detalhe"
                        data-bs-toggle="modal" data-bs-target="#modalDetalhe"
                        data-oferta-idx="${i}">
                  <i class="bi bi-eye me-1"></i>Ver
                </button>
              </div>
            </div>
          </div>
        </div>`;
    });
    $('#produtosGrid').html(html);

    // store for modal reference
    window._ofertas = lista;
  }

  /* ---- Filtros ---- */
  $('#btnFiltrar').on('click', function () {
    const params = {};
    const search   = $('#searchProduto').val().trim();
    const prov     = $('#filtroProvincia').val();
    const cat      = $('#filtroCategoria').val();
    if (search) params.q         = search;
    if (prov)   params.provincia = prov;
    if (cat)    params.categoria = cat;
    loadOfertas(params);
  });

  $('#searchProduto').on('keypress', function (e) {
    if (e.which === 13) $('#btnFiltrar').trigger('click');
  });

  /* ---- Modal detalhe: preencher com dados da oferta ---- */
  $(document).on('click', '.btn-ver-detalhe', function () {
    const idx = $(this).data('oferta-idx');
    const o   = window._ofertas ? window._ofertas[idx] : null;
    if (!o) return;

    $('#modalNomeProduto').text(o.nomeProduto);
    $('#modalCategoria').text(o.categoriaNome || '—');
    $('#modalDescricao').text(o.descricao || 'Sem descrição.');
    $('#modalQuantidade').text(`${o.quantidadeRestante} ${o.unidade} disponíveis`);
    $('#modalPreco').text(`MT ${parseFloat(o.preco || 0).toFixed(2)}/${o.unidade}`);
    $('#modalLocal').text([o.bairro, o.distrito, o.provincia].filter(Boolean).join(', ') || '—');
    $('#modalProdutor').text(o.nomeProdutor || '—');
    $('#modalTelefone').text(o.telefoneProdutor || '—');
    $('#modalValidade').text(o.dataValidade || '—');

    // Reset pedido form
    $('#qtdPedido').val('');
    $('#totalEstimado').val('');
    $('#btnConfirmarPedido').data('oferta', o);
  });

  /* ---- Calcular total ao digitar quantidade ---- */
  $('#qtdPedido').on('input', function () {
    const oferta = $('#btnConfirmarPedido').data('oferta');
    if (!oferta) return;
    const qty   = parseFloat($(this).val()) || 0;
    const total = (qty * parseFloat(oferta.preco || 0)).toFixed(2);
    $('#totalEstimado').val(total);
  });

  /* ---- Confirmar pedido de compra ---- */
  $('#btnConfirmarPedido').on('click', function () {
    const oferta = $(this).data('oferta');
    const qty    = parseFloat($('#qtdPedido').val());
    if (!qty || qty <= 0) {
      showToast('Introduza uma quantidade válida.', 'warning');
      return;
    }

    const payload = {
      compradorId: parseInt(compradorId),
      ofertaId:    oferta.idOferta,
      quantidade:  qty,
      dataPedido:  new Date().toISOString()
    };

    $.ajax({
      url: '/api/pedidos',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload)
    })
    .done(function () {
      bootstrap.Modal.getInstance(document.getElementById('modalDetalhe')).hide();
      showToast('Pedido enviado com sucesso!', 'success');
      loadMeusPedidos();
    })
    .fail(function (xhr) {
      const msg = xhr.responseJSON?.message || 'Erro ao enviar pedido.';
      showToast(msg, 'error');
    });
  });

  /* ---- Favoritos ---- */
  $('#btnFavorito').on('click', function () {
    const oferta = $('#btnConfirmarPedido').data('oferta');
    if (!oferta) return;
    let favs = JSON.parse(localStorage.getItem('favoritos') || '[]');
    const exists = favs.find(f => f.idOferta === oferta.idOferta);
    if (!exists) {
      favs.push(oferta);
      localStorage.setItem('favoritos', JSON.stringify(favs));
      showToast('Adicionado aos favoritos!', 'success');
      $(this).html('<i class="bi bi-heart-fill me-1"></i>Nos Favoritos').addClass('btn-danger').removeClass('btn-outline-danger');
    } else {
      showToast('Este produto já está nos favoritos.', 'warning');
    }
  });

  /* ---- Tab: Meus Pedidos ---- */
  $(document).on('click', '.sidebar-link[data-tab="meus-pedidos"]', function () {
    loadMeusPedidos();
  });

  function loadMeusPedidos(estado = '') {
    const url = `/api/compradores/${compradorId}/pedidos${estado ? '?estado=' + estado : ''}`;
    $.get(url)
      .done(function (data) { renderMeusPedidos(data); })
      .fail(function () { /* sem dados */ });
  }

  $('#filtroMeusPedidos').on('change', function () {
    loadMeusPedidos($(this).val());
  });

  function renderMeusPedidos(lista) {
    if (!lista || lista.length === 0) return;
    let html = '';
    lista.forEach(function (p, i) {
      const badgeClass = p.estado === 'aceite' ? 'bg-success'
                       : p.estado === 'rejeitado' ? 'bg-danger' : 'bg-warning text-dark';
      html += `
        <tr>
          <td>${i + 1}</td>
          <td>${p.nomeProduto}</td>
          <td>${p.nomeProdutor}</td>
          <td>${p.dataPedido}</td>
          <td>${p.dataResposta || '—'}</td>
          <td><span class="badge ${badgeClass}">${p.estado}</span></td>
          <td>
            ${p.estado === 'pendente' ? `
              <button class="btn btn-sm btn-outline-danger btn-cancelar" data-id="${p.idPedido}">
                <i class="bi bi-x-circle me-1"></i>Cancelar
              </button>` : '—'}
          </td>
        </tr>`;
    });
    $('#tbodyMeusPedidos').html(html);
  }

  /* ---- Tab: Favoritos ---- */
  $(document).on('click', '.sidebar-link[data-tab="favoritos"]', function () {
    const favs = JSON.parse(localStorage.getItem('favoritos') || '[]');
    if (favs.length === 0) return;
    let html = '';
    favs.forEach(function (o) {
      html += `
        <div class="col-sm-6 col-xl-4">
          <div class="product-card">
            <div class="product-card-icon"><i class="bi bi-heart-fill text-danger"></i></div>
            <div class="product-card-body p-3">
              <h6 class="product-name">${o.nomeProduto}</h6>
              <p class="product-desc text-muted small">${o.descricao || ''}</p>
              <div class="product-price">MT ${parseFloat(o.preco || 0).toFixed(2)}/${o.unidade}</div>
            </div>
          </div>
        </div>`;
    });
    $('#favoritosGrid').html(html);
  });

});
