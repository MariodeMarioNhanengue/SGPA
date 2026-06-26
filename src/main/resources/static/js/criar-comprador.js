$(function () {

  const step1 = $('#step1C');
  const step2 = $('#step2C');
  let geoInicializado = false;

  // Próximo
  $('#nextC').on('click', function () {

    if (!$('#nomeC').val() || !$('#apelidoC').val() || !$('#telefoneC').val()) {
      showToast('Preencha todos os campos obrigatórios', 'warning');
      return;
    }

    step1.addClass('d-none');
    step2.removeClass('d-none');

    // Inicializar os selects de localização AGORA que estão visíveis no DOM
    if (!geoInicializado && typeof inicializarGeo === 'function') {
      inicializarGeo('provinciaC', 'distritoC', 'bairroC', '', '', '');
      geoInicializado = true;
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // Voltar
  $('#prevC').on('click', function () {
    step2.addClass('d-none');
    step1.removeClass('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

});
