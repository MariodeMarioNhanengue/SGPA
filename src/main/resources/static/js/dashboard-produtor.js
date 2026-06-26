/* ================================================
   dashboard-comprador.js  –  CU13 a CU17
   Sistema de Gestão de Produtos Agrícolas (SGPA)
================================================ */
$(function () {

  /* ── IDs do comprador (vindos do HTML Thymeleaf) ── */
  const hiddenEl       = document.getElementById('compradorIdHidden');
  const compradorId    = (hiddenEl && hiddenEl.dataset.id)   || sessionStorage.getItem('compradorId')   || 1;
  const compradorNome  = (hiddenEl && hiddenEl.dataset.nome) || sessionStorage.getItem('compradorNome') || 'Comprador';

  if (compradorId) sessionStorage.setItem('compradorId',   compradorId);
  if (compradorNome) sessionStorage.setItem('compradorNome', compradorNome);
  $('#sidebarNome').text(compradorNome);

  /* ── Carregar ofertas ao iniciar (CU13) ── */
  loadOfertas();

  /* ==================================================
     CU13 – Fazer buscas
     CU14 – Filtrar por província
     CU15 – Filtrar por tipo de produto
  ================================================== */
  function loadOfertas(params = {}) {
    const qs = $.param(params);
    $.get('/api/ofertas?' + qs)
      .done(function (data) { renderOfertas(data); })
      .fail(function () {
        $('#resultCount').text('Não foi possível ligar à API. A mostrar produtos de demonstração.');
        renderOfertas(getDemoOfertas());
      });
  }

  /* ── Botão Filtrar ── */
  $('#btnFiltrar').on('click', function () {
    const params   = {};
    const search   = $('#searchProduto').val().trim();
    const prov     = $('#filtroProvincia').val();
    const cat      = $('#filtroCategoria').val();
    if (search) params.q         = search;
    if (prov)   params.provincia = prov;
    if (cat)    params.categoria = cat;
    loadOfertas(params);
  });

  /* Enter na barra de pesquisa dispara filtro */
  $('#searchProduto').on('keypress', function (e) {
    if (e.which === 13) $('#btnFiltrar').trigger('click');
  });

  /* ── Render dos cards de produtos ── */
  function renderOfertas(lista) {
    if (!lista || lista.length === 0) {
      $('#produtosGrid').html(
        '<div class="col-12 text-center text-muted py-5">' +
        '<i class="bi bi-search fs-2 d-block mb-2"></i>Nenhuma oferta encontrada.</div>'
      );
      $('#resultCount').text('0 ofertas encontradas.');
      return;
    }

    $('#resultCount').text(lista.length + ' oferta(s) encontrada(s).');
    const icons  = ['bi-basket2-fill','bi-apple','bi-flower1','bi-egg-fill','bi-tree'];
    const themes = ['','alt','green','','alt'];
    let html = '';

    lista.forEach(function (o, i) {
      const icon      = icons[i % icons.length];
      const theme     = themes[i % themes.length];
      const qtdRest   = parseFloat(o.quantidadeRestante || 0);
      const qtdTotal  = parseFloat(o.quantidadeTotal    || 1);
      const baixo     = qtdRest < qtdTotal * 0.2;
      const badgeTxt  = baixo ? 'A esgotar' : 'Activo';
      const badgeCls  = baixo ? 'warning'   : '';

      html += `
        <div class="col-sm-6 col-xl-4">
          <div class="product-card" data-idx="${i}">
            <div class="product-card-badge ${badgeCls}">${badgeTxt}</div>
            <div class="product-card-icon ${theme}"><i class="bi ${icon}"></i></div>
            <div class="product-card-body p-3">
              <div class="d-flex justify-content-between align-items-start mb-1">
                <h6 class="product-name mb-0">${o.nomeProduto || '—'}</h6>
                <span class="product-category-tag">${o.categoria || o.categoriaNome || '—'}</span>
              </div>
              <p class="product-desc text-muted small mb-2">${o.descricao || 'Sem descrição.'}</p>
              <div class="product-meta d-flex gap-3 mb-3">
                <span><i class="bi bi-stack me-1 text-accent"></i>${qtdRest} ${o.unidade || ''}</span>
                <span><i class="bi bi-geo-alt me-1 text-accent"></i>${o.provincia || o.bairro || '—'}</span>
              </div>
              <div class="d-flex justify-content-between align-items-center">
                <div class="product-price">
                  MT ${parseFloat(o.preco || 0).toFixed(2)}
                  <span class="small text-muted">/${o.unidade || 'un'}</span>
                </div>
                <button class="btn btn-accent-blue btn-sm btn-ver-detalhe"
                        data-bs-toggle="modal" data-bs-target="#modalDetalhe"
                        data-idx="${i}">
                  <i class="bi bi-eye me-1"></i>Ver
                </button>
              </div>
            </div>
          </div>
        </div>`;
    });

    $('#produtosGrid').html(html);
    window._ofertas = lista;
  }

  /* ==================================================
     CU16 – Visualizar detalhes do produto
  ================================================== */
  $(document).on('click', '.btn-ver-detalhe', function () {
    const idx = $(this).data('idx');
    const o   = window._ofertas ? window._ofertas[idx] : null;
    if (!o) return;

    $('#modalNomeProduto').text(o.nomeProduto || '—');
    $('#modalCategoria').text(o.categoria || o.categoriaNome || '—');
    $('#modalDescricao').text(o.descricao || 'Sem descrição.');
    $('#modalQuantidade').text((o.quantidadeRestante || 0) + ' ' + (o.unidade || 'un') + ' disponíveis');
    $('#modalPreco').text('MT ' + parseFloat(o.preco || 0).toFixed(2) + '/' + (o.unidade || 'un'));
    $('#modalValidade').text(o.dataValidade ? o.dataValidade.substring(0,10) : '—');
    $('#modalLocal').text([o.bairro, o.distrito, o.provincia].filter(Boolean).join(', ') || '—');
    $('#modalProdutor').text(o.nomeProdutor || '—');
    $('#modalTelefone').text(o.telefoneProdutor || '—');
    $('#unidadePedido').text(o.unidade || 'un');

    /* Reset formulário de pedido */
    $('#qtdPedido').val('');
    $('#totalEstimado').val('');
    $('#btnConfirmarPedido').data('oferta', o);

    /* Reset botão favorito */
    const favs   = JSON.parse(localStorage.getItem('favoritos') || '[]');
    const jaFav  = favs.some(f => f.idOferta === o.idOferta || f.id === o.id);
    if (jaFav) {
      $('#btnFavorito').html('<i class="bi bi-heart-fill"></i>').addClass('btn-danger').removeClass('btn-outline-danger');
    } else {
      $('#btnFavorito').html('<i class="bi bi-heart"></i>').removeClass('btn-danger').addClass('btn-outline-danger');
    }
  });

  /* Calcular total estimado enquanto digita quantidade */
  $('#qtdPedido').on('input', function () {
    const oferta = $('#btnConfirmarPedido').data('oferta');
    if (!oferta) return;
    const qty   = parseFloat($(this).val()) || 0;
    const total = (qty * parseFloat(oferta.preco || 0)).toFixed(2);
    $('#totalEstimado').val(total > 0 ? total : '');
  });

  /* ==================================================
     CU17 – Realizar pedido de compra
  ================================================== */
  $('#btnConfirmarPedido').on('click', function () {
    const oferta = $(this).data('oferta');
    if (!oferta) { showToast('Seleccione um produto primeiro.', 'warning'); return; }

    const qty = parseFloat($('#qtdPedido').val());
    if (!qty || qty <= 0) { showToast('Introduza uma quantidade válida.', 'warning'); return; }

    const qtdDisp = parseFloat(oferta.quantidadeRestante || 0);
    if (qty > qtdDisp) {
      showToast('Quantidade pedida excede o stock disponível (' + qtdDisp + ' ' + (oferta.unidade||'un') + ').', 'warning');
      return;
    }

    const payload = {
      compradorId : parseInt(compradorId),
      ofertaId    : oferta.idOferta || oferta.id,
      quantidade  : qty,
      dataPedido  : new Date().toISOString()
    };

    $('#btnConfirmarPedido').prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span>A enviar...');

    $.ajax({
      url         : '/api/pedidos',
      method      : 'POST',
      contentType : 'application/json',
      data        : JSON.stringify(payload)
    })
    .done(function () {
      bootstrap.Modal.getInstance(document.getElementById('modalDetalhe')).hide();
      showToast('Pedido enviado com sucesso! O produtor será notificado.', 'success');
      loadMeusPedidos();
      loadOfertas(); // actualizar stocks
    })
    .fail(function (xhr) {
      const msg = (xhr.responseJSON && xhr.responseJSON.message) || 'Erro ao enviar pedido. Tente novamente.';
      showToast(msg, 'error');
    })
    .always(function () {
      $('#btnConfirmarPedido').prop('disabled', false).html('<i class="bi bi-send-fill me-1"></i>Enviar Pedido');
    });
  });

  /* ── Favoritos ── */
  $('#btnFavorito').on('click', function () {
    const oferta = $('#btnConfirmarPedido').data('oferta');
    if (!oferta) return;
    let favs  = JSON.parse(localStorage.getItem('favoritos') || '[]');
    const key = oferta.idOferta || oferta.id;
    const idx = favs.findIndex(f => (f.idOferta || f.id) === key);
    if (idx === -1) {
      favs.push(oferta);
      localStorage.setItem('favoritos', JSON.stringify(favs));
      showToast('Adicionado aos favoritos!', 'success');
      $(this).html('<i class="bi bi-heart-fill"></i>').addClass('btn-danger').removeClass('btn-outline-danger');
    } else {
      favs.splice(idx, 1);
      localStorage.setItem('favoritos', JSON.stringify(favs));
      showToast('Removido dos favoritos.', 'warning');
      $(this).html('<i class="bi bi-heart"></i>').removeClass('btn-danger').addClass('btn-outline-danger');
    }
  });

  /* ── Tab Meus Pedidos ── */
  $(document).on('click', '.sidebar-link[data-tab="meus-pedidos"]', function () { loadMeusPedidos(); });
  $('#filtroMeusPedidos').on('change', function () { loadMeusPedidos($(this).val()); });

  function loadMeusPedidos(estado) {
    const url = '/api/compradores/' + compradorId + '/pedidos' + (estado ? '?estado=' + estado : '');
    $.get(url)
      .done(function (data) { renderMeusPedidos(data); })
      .fail(function () { /* manter tabela vazia */ });
  }

  function renderMeusPedidos(lista) {
    if (!lista || lista.length === 0) {
      $('#tbodyMeusPedidos').html(
        '<tr><td colspan="7" class="text-center text-muted py-4">' +
        '<i class="bi bi-bag fs-3 d-block mb-2 opacity-50"></i>Nenhum pedido encontrado.</td></tr>'
      );
      return;
    }
    let html = '';
    lista.forEach(function (p, i) {
      const bc = p.estado === 'ACEITE'    ? 'bg-success'
               : p.estado === 'REJEITADO' ? 'bg-danger'
               :                            'bg-warning text-dark';
      const cancelBtn = p.estado === 'PENDENTE'
        ? `<button class="btn btn-sm btn-outline-danger btn-cancelar" data-id="${p.idPedido||p.id}"><i class="bi bi-x-circle me-1"></i>Cancelar</button>`
        : '—';
      html += `<tr>
        <td>${i + 1}</td>
        <td>${p.nomeProduto || '—'}</td>
        <td>${p.nomeProdutor || '—'}</td>
        <td>${p.dataPedido ? p.dataPedido.substring(0,10) : '—'}</td>
        <td>${p.dataResposta ? p.dataResposta.substring(0,10) : '—'}</td>
        <td><span class="badge ${bc}">${p.estado || '—'}</span></td>
        <td>${cancelBtn}</td>
      </tr>`;
    });
    $('#tbodyMeusPedidos').html(html);
  }

  /* ── Tab Favoritos ── */
  $(document).on('click', '.sidebar-link[data-tab="favoritos"]', renderFavoritos);

  function renderFavoritos() {
    const favs = JSON.parse(localStorage.getItem('favoritos') || '[]');
    if (favs.length === 0) {
      $('#favoritosGrid').html(
        '<div class="col-12 text-center text-muted py-5">' +
        '<i class="bi bi-heart fs-2 d-block mb-2 opacity-40"></i>Nenhum favorito ainda.</div>'
      );
      return;
    }
    let html = '';
    favs.forEach(function (o) {
      html += `
        <div class="col-sm-6 col-xl-4">
          <div class="product-card">
            <div class="product-card-icon"><i class="bi bi-heart-fill text-danger"></i></div>
            <div class="product-card-body p-3">
              <h6 class="product-name">${o.nomeProduto || '—'}</h6>
              <p class="product-desc text-muted small">${o.descricao || ''}</p>
              <div class="product-price mb-2">MT ${parseFloat(o.preco||0).toFixed(2)}/${o.unidade||'un'}</div>
              <div class="d-flex gap-2">
                <button class="btn btn-accent-blue btn-sm flex-grow-1 btn-ver-fav"
                        data-bs-toggle="modal" data-bs-target="#modalDetalhe"
                        data-oferta='${JSON.stringify(o)}'>
                  <i class="bi bi-eye me-1"></i>Ver
                </button>
              </div>
            </div>
          </div>
        </div>`;
    });
    $('#favoritosGrid').html(html);
  }

  /* Click no card de favorito abre o modal */
  $(document).on('click', '.btn-ver-fav', function () {
    const o = $(this).data('oferta');
    if (!o) return;
    window._ofertas = window._ofertas || [];
    const idx = window._ofertas.findIndex(x => (x.idOferta||x.id) === (o.idOferta||o.id));
    if (idx === -1) { window._ofertas.push(o); }
    const realIdx = idx === -1 ? window._ofertas.length - 1 : idx;
    // Simula click no detalhe
    setTimeout(function () {
      const tmpO = window._ofertas[realIdx];
      $('#modalNomeProduto').text(tmpO.nomeProduto || '—');
      $('#modalCategoria').text(tmpO.categoria || '—');
      $('#modalDescricao').text(tmpO.descricao || '—');
      $('#modalQuantidade').text((tmpO.quantidadeRestante||0)+' '+(tmpO.unidade||'un'));
      $('#modalPreco').text('MT '+parseFloat(tmpO.preco||0).toFixed(2)+'/'+(tmpO.unidade||'un'));
      $('#modalValidade').text(tmpO.dataValidade ? tmpO.dataValidade.substring(0,10) : '—');
      $('#modalLocal').text([tmpO.bairro,tmpO.distrito,tmpO.provincia].filter(Boolean).join(', ')||'—');
      $('#modalProdutor').text(tmpO.nomeProdutor||'—');
      $('#modalTelefone').text(tmpO.telefoneProdutor||'—');
      $('#unidadePedido').text(tmpO.unidade||'un');
      $('#qtdPedido').val(''); $('#totalEstimado').val('');
      $('#btnConfirmarPedido').data('oferta', tmpO);
    }, 50);
  });

  /* Limpar favoritos */
  $('#btnLimparFavoritos').on('click', function () {
    if (confirm('Tem certeza que quer remover todos os favoritos?')) {
      localStorage.removeItem('favoritos');
      renderFavoritos();
    }
  });

  /* ── Toggle Grid / List ── */
  $('#viewGrid').on('click', function () {
    $('#produtosGrid').removeClass('list-view');
    $(this).addClass('active'); $('#viewList').removeClass('active');
  });
  $('#viewList').on('click', function () {
    $('#produtosGrid').addClass('list-view');
    $(this).addClass('active'); $('#viewGrid').removeClass('active');
  });

  /* ── Toast helper ── */
  function showToast(msg, tipo) {
    const el  = document.getElementById('toastMsg');
    const cls = tipo === 'success' ? 'bg-success text-white'
              : tipo === 'error'   ? 'bg-danger text-white'
              :                      'bg-warning text-dark';
    el.className = 'toast align-items-center border-0 ' + cls;
    document.getElementById('toastText').textContent = msg;
    bootstrap.Toast.getOrCreateInstance(el, { delay: 3500 }).show();
  }

  /* ── Dados de demonstração (caso API esteja em baixo) ── */
  function getDemoOfertas() {
    return [
      { id:1, idOferta:1, nomeProduto:'Milho Branco', categoria:'Cereais', descricao:'Milho de qualidade superior, colheita de Março 2026.',
        quantidadeTotal:500, quantidadeRestante:320, preco:15.50, unidade:'kg', provincia:'Gaza', nomeProdutor:'António Machava' },
      { id:2, idOferta:2, nomeProduto:'Tomate Fresco', categoria:'Hortícolas', descricao:'Tomate vermelho maduro, ideal para mercado.',
        quantidadeTotal:200, quantidadeRestante:180, preco:35.00, unidade:'kg', provincia:'Maputo', nomeProdutor:'Maria José' },
      { id:3, idOferta:3, nomeProduto:'Manga Keitt', categoria:'Frutas', descricao:'Manga doce da variedade Keitt, grande e suculenta.',
        quantidadeTotal:300, quantidadeRestante:50, preco:20.00, unidade:'kg', provincia:'Inhambane', nomeProdutor:'João Cossa' }
    ];
  }

});