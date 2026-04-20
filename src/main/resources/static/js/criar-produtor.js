/* ================================================
   criar-produtor.js
   Handles form validation + submit to Spring Boot API
================================================ */
$(function () {

  $('#formProdutor').on('submit', function (e) {
    e.preventDefault();

    // Bootstrap native validation
    if (!this.checkValidity()) {
      $(this).addClass('was-validated');
      return;
    }

    // Collect data
    const payload = {
      nome:      $('#nome').val().trim() + ' ' + $('#apelido').val().trim(),
      telefone:  $('#telefone').val().trim(),
      email:     $('#email').val().trim(),
      nuit:      $('#nuit').val().trim(),
     provincia: $('#provinciaC').val(),
distrito:  $('#distritoC').val(),
bairro:    $('#bairroC').val()
    };

    // Loading state
    $('#btnSubmit .btn-text').addClass('d-none');
    $('#btnSubmit .btn-loading').removeClass('d-none');
    $('#btnSubmit').prop('disabled', true);

    // POST to Spring Boot endpoint
    $.ajax({
      url: '/api/produtores',   // Spring Boot REST endpoint
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload),
      success: function (response) {
        // Guardar ID na sessão (simulado via sessionStorage)
        sessionStorage.setItem('produtorId',   response.idProdutor || 1);
        sessionStorage.setItem('produtorNome', payload.nome);
        showToast('Conta criada com sucesso! A redirecionar...', 'success');
        setTimeout(() => {window.location.href = '/produtor/dashboard'; }, 1500);
      },
      error: function (xhr) {
        resetBtn();
        const msg = xhr.responseJSON?.message || 'Erro ao criar conta. Verifique os dados.';
        showToast(msg, 'error');
      }
    });
  });

  function resetBtn() {
    $('#btnSubmit .btn-text').removeClass('d-none');
    $('#btnSubmit .btn-loading').addClass('d-none');
    $('#btnSubmit').prop('disabled', false);
  }

  /* --- Demo mode: se API não disponível, redireciona na mesma --- */
  // Remova este bloco quando ligar ao back-end real
  $(document).on('ajaxError', function () {});

});
$(document).ready(function () {

    // ===============================
    // TROCAR TABS
    // ===============================
    $('.sidebar-link, [data-tab-link]').click(function (e) {
        e.preventDefault();

        let tab = $(this).data('tab') || $(this).data('tab-link');

        $('.tab-pane-content').removeClass('active');
        $('#tab-' + tab).addClass('active');

        $('.sidebar-link').removeClass('active');
        $('.sidebar-link[data-tab="' + tab + '"]').addClass('active');

        $('#pageTitle').text($(this).text().trim());
    });

    // ===============================
    // ABRIR MODAL EDITAR
    // ===============================
    $('[data-bs-target="#modalEditar"]').click(function () {
        let id = $(this).data('id');
        $('#editOfertaId').val(id);
    });

    // ===============================
    // SALVAR EDIÇÃO (AJAX)
    // ===============================
    $('#btnSalvarEdicao').click(function () {

        let id = $('#editOfertaId').val();
        let preco = $('#editPreco').val();
        let quantidade = $('#editQuantidade').val();
        let token = $('#csrfToken').val();

        $.ajax({
            url: '/produtor/editar-oferta/' + id,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                preco: preco,
                quantidadeTotal: quantidade
            }),
            beforeSend: function (xhr) {
                xhr.setRequestHeader('X-CSRF-TOKEN', token);
            },
            success: function () {
                alert('Oferta actualizada com sucesso!');
                location.reload();
            },
            error: function () {
                alert('Erro ao actualizar oferta.');
            }
        });
    });

    // ===============================
    // APAGAR OFERTA
    // ===============================
    $('.btn-outline-danger').click(function () {

        let id = $(this).data('id');

        if (!confirm('Tem certeza que deseja eliminar esta oferta?')) return;

        $.post('/produtor/apagar-oferta/' + id, function () {
            alert('Oferta removida!');
            location.reload();
        });
    });

    // ===============================
    // ACEITAR PEDIDO
    // ===============================
    $('.btn-success').click(function () {

        let id = $(this).data('id');

        $.post('/produtor/aceitar-pedido/' + id, function () {
            alert('Pedido aceite!');
            location.reload();
        });
    });

    // ===============================
    // REJEITAR PEDIDO
    // ===============================
    $('.btn-danger').click(function () {

        let id = $(this).data('id');

        $.post('/produtor/rejeitar-pedido/' + id, function () {
            alert('Pedido rejeitado!');
            location.reload();
        });
    });

});
// ===============================
// NAVEGAÇÃO ENTRE TABS
// ===============================
$('.sidebar-link').click(function (e) {
    e.preventDefault();

    let tab = $(this).data('tab');
    let title = $(this).data('title');

    // remover active de todos
    $('.sidebar-link').removeClass('active');
    $(this).addClass('active');

    // esconder todas tabs
    $('.tab-pane-content').removeClass('active');

    // mostrar a tab certa
    $('#tab-' + tab).addClass('active');

    // mudar título
    $('#pageTitle').text(title);
});
