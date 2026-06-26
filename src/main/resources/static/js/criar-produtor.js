$(function () {

  const step1 = $('#step-1');
  const step2 = $('#step-2');

  // Próximo
  $('#nextStep').on('click', function () {

    // validação simples
    if (!$('#nome').val() || !$('#apelido').val() || !$('#telefone').val()) {
      showToast('Preencha todos os campos obrigatórios', 'warning');
      return;
    }

    step1.addClass('d-none');
    step2.removeClass('d-none');

    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // Voltar
  $('#prevStep').on('click', function () {
    step2.addClass('d-none');
    step1.removeClass('d-none');

    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

});