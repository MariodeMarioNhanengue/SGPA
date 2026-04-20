/* ================================================
   criar-comprador.js
   Handles form validation + submit to Spring Boot API
================================================ */
$(function () {

  /* --- Confirm password check --- */
  $('#confirmarSenha').on('input', function () {
    const senha    = $('#senhaC').val();
    const confirm  = $(this).val();
    if (confirm && senha !== confirm) {
      this.setCustomValidity('Senhas não coincidem');
      $('#senhaMatchMsg').text('As senhas não coincidem.');
    } else {
      this.setCustomValidity('');
    }
  });

  $('#formComprador').on('submit', function (e) {
    e.preventDefault();

    // Confirm password
    const senha   = $('#senhaC').val();
    const confirm = $('#confirmarSenha').val();
    if (senha !== confirm) {
      $('#confirmarSenha')[0].setCustomValidity('Senhas não coincidem');
      $(this).addClass('was-validated');
      return;
    }

    if (!this.checkValidity()) {
      $(this).addClass('was-validated');
      return;
    }

    // Collect data
    const payload = {
      nome:      $('#nomeC').val().trim() + ' ' + $('#apelidoC').val().trim(),
      telefone:  $('#telefoneC').val().trim(),
      email:     $('#emailC').val().trim(),
      senha:     senha,
      provincia: $('#provinciaC').val(),
      distrito:  $('#distritoC').val().trim(),
      bairro:    $('#bairroC').val().trim()
    };

    // Loading state
    $('#btnSubmitC .btn-text').addClass('d-none');
    $('#btnSubmitC .btn-loading').removeClass('d-none');
    $('#btnSubmitC').prop('disabled', true);

    // POST to Spring Boot endpoint
    $.ajax({
      url: '/api/compradores',   // Spring Boot REST endpoint
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload),
      success: function (response) {
        sessionStorage.setItem('compradorId',   response.idComprador || 1);
        sessionStorage.setItem('compradorNome', payload.nome);
        showToast('Conta criada com sucesso! A redirecionar...', 'success');
        setTimeout(() => { window.location.href = 'dashboard-comprador.html'; }, 1500);
      },
      error: function (xhr) {
        resetBtn();
        const msg = xhr.responseJSON?.message || 'Erro ao criar conta. Verifique os dados.';
        showToast(msg, 'error');
      }
    });
  });

  function resetBtn() {
    $('#btnSubmitC .btn-text').removeClass('d-none');
    $('#btnSubmitC .btn-loading').addClass('d-none');
    $('#btnSubmitC').prop('disabled', false);
  }

});
