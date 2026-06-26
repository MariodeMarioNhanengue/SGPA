/* ================================================
   SGPA – main.js (VERSÃO FINAL CORRIGIDA)
================================================ */

/* ================= HERO CAROUSEL ================= */
document.addEventListener("DOMContentLoaded", function () {

  const carousel = document.querySelector('#heroCarousel');

  if (carousel && window.bootstrap) {

    const hero = new bootstrap.Carousel(carousel, {
      interval: 3500,     // troca mais fluida
      ride: "carousel",
      pause: false,       // NÃO para ao passar mouse
      wrap: true,         // loop infinito
      touch: true
    });

    // FORÇA o autoplay mesmo após interação
    carousel.addEventListener('mouseenter', () => hero.cycle());
    carousel.addEventListener('mouseleave', () => hero.cycle());
  }
});


/* ================= jQuery SAFE INIT ================= */
$(function () {

  /* ================= NAVBAR SCROLL ================= */
  $(window).on('scroll', function () {
    $('#mainNav').toggleClass('scrolled', $(this).scrollTop() > 40);
  });

  /* ================= SIDEBAR TOGGLE ================= */
  $('#sidebarToggle').on('click', function () {
    $('#sidebar').toggleClass('show');
  });

  /* ================= TAB NAVIGATION ================= */
  $(document).on('click', '.sidebar-link[data-tab]', function (e) {
    e.preventDefault();

    const tab = $(this).data('tab');

    switchTab(tab);

    $('.sidebar-link').removeClass('active');
    $(this).addClass('active');

    $('#sidebar').removeClass('show');
  });

  /* ================= QUICK LINKS ================= */
  $(document).on('click', '[data-tab-link]', function (e) {
    e.preventDefault();

    const tab = $(this).data('tab-link');

    switchTab(tab);

    $('.sidebar-link').removeClass('active');
    $(`.sidebar-link[data-tab="${tab}"]`).addClass('active');
  });

  /* ================= SWITCH TAB ================= */
  function switchTab(tab) {
    $('.tab-pane-content').removeClass('active');
    $(`#tab-${tab}`).addClass('active');

    const label = $(`.sidebar-link[data-tab="${tab}"]`).text().trim();
    $('#pageTitle').text(label || '');
  }

  /* ================= PASSWORD TOGGLE ================= */
  function togglePassword(input, icon) {
    if (!input.length) return;

    if (input.attr('type') === 'password') {
      input.attr('type', 'text');
      icon.removeClass('bi-eye').addClass('bi-eye-slash');
    } else {
      input.attr('type', 'password');
      icon.removeClass('bi-eye-slash').addClass('bi-eye');
    }
  }

  $('#toggleSenha').on('click', function () {
    togglePassword($('#senha'), $('#senhaIcon'));
  });

  $('#toggleSenhaC').on('click', function () {
    togglePassword($('#senhaC'), $('#senhaCIcon'));
  });

  /* ================= INPUT MASKS ================= */
  if ($.fn.mask) {
    $('.phone-mask').mask('00 000 0000');
    $('.nuit-mask').mask('000 000 000');
  }

  /* ================= SET TODAY DATE ================= */
  const today = new Date().toISOString().split('T')[0];

  $('#dataPublicacao').val(function (_, val) {
    return val || today;
  });

  /* ================= VIEW TOGGLE ================= */
  $('#viewGrid').on('click', function () {
    $('#produtosGrid').removeClass('list-view');
    $('#viewList').removeClass('active');
    $(this).addClass('active');
  });

  $('#viewList').on('click', function () {
    $('#produtosGrid').addClass('list-view');
    $('#viewGrid').removeClass('active');
    $(this).addClass('active');
  });

  /* ================= TOAST SYSTEM ================= */
  window.showToast = function (msg, type = 'success') {

    const id = 'toast-' + Date.now();

    const config = {
      success: { color: '#2ecc71', icon: 'bi-check-circle-fill' },
      error:   { color: '#e74c3c', icon: 'bi-x-circle-fill' },
      warning: { color: '#f39c12', icon: 'bi-exclamation-circle-fill' }
    }[type] || {
      color: '#2ecc71',
      icon: 'bi-check-circle-fill'
    };

    const html = `
      <div id="${id}" class="toast align-items-center text-white border-0"
           role="alert"
           style="background:${config.color};
                  position:fixed;
                  bottom:20px;
                  right:20px;
                  z-index:99999;
                  min-width:260px;
                  box-shadow:0 10px 25px rgba(0,0,0,0.2);">

        <div class="d-flex">
          <div class="toast-body d-flex align-items-center gap-2">
            <i class="bi ${config.icon}"></i>
            <span>${msg}</span>
          </div>

          <button type="button"
                  class="btn-close btn-close-white me-2 m-auto"
                  data-bs-dismiss="toast"></button>
        </div>
      </div>
    `;

    $('body').append(html);

    const toastEl = document.getElementById(id);

    const toast = new bootstrap.Toast(toastEl, {
      delay: 3000,
      autohide: true
    });

    toast.show();

    toastEl.addEventListener('hidden.bs.toast', () => {
      toastEl.remove();
    });
  };

});
/* ================= PAGE OVERLAY SYSTEM ================= */
$(document).on('click', '[data-page]', function (e) {
  e.preventDefault();

  const page = $(this).data('page');

  $('#overlayBody').html('<p class="text-center">Carregando...</p>');

  $('#pageOverlay').addClass('active');

  // CARREGA HTML
  $('#overlayBody').load(page + ' body > *', function (response, status) {

    if (status === "error") {
      $('#overlayBody').html('<p class="text-danger text-center">Erro ao carregar página.</p>');
    }

  });

});

/* ================= CLOSE OVERLAY ================= */
$('#closeOverlay').on('click', function () {
  $('#pageOverlay').removeClass('active');
});

/* FECHAR AO CLICAR FORA */
$('#pageOverlay').on('click', function (e) {
  if (e.target.id === 'pageOverlay') {
    $(this).removeClass('active');
  }
});