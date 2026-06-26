/* minhas-ofertas.js – filtro da tabela de ofertas */
function filterTable() {
  const q      = document.getElementById('searchInput').value.toLowerCase();
  const estado = document.getElementById('filterEstado').value;
  const cat    = document.getElementById('filterCategoria').value;
  const rows   = document.querySelectorAll('#tabelaOfertas tbody tr[data-produto]');
  let visible  = 0;

  rows.forEach(row => {
    const match =
      (!q      || row.dataset.produto.toLowerCase().includes(q)) &&
      (!estado  || row.dataset.estado    === estado) &&
      (!cat     || row.dataset.categoria === cat);

    row.style.display = match ? '' : 'none';
    if (match) visible++;
  });

  document.getElementById('emptyFilter').style.display = visible === 0 ? '' : 'none';
  document.getElementById('totalInfo').textContent =
    visible + ' oferta' + (visible !== 1 ? 's' : '') + ' encontrada' + (visible !== 1 ? 's' : '');
}

document.addEventListener('DOMContentLoaded', filterTable);
