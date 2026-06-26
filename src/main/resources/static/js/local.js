document.addEventListener("DOMContentLoaded", function () {

    const provincia = document.getElementById("provinciaC");
    const distrito  = document.getElementById("distritoC");
    const bairro    = document.getElementById("bairroC");

    // ================= PROVÍNCIAS =================
    fetch("/api/provincias")
        .then(r => {
            if (!r.ok) throw new Error("Erro ao carregar províncias");
            return r.json();
        })
        .then(data => {
            provincia.innerHTML = '<option value="">Selecione</option>';

            data.forEach(p => {
                provincia.innerHTML += `<option value="${p.id}">${p.nome}</option>`;
            });
        })
        .catch(err => console.error(err));

    // ================= DISTRITOS =================
    provincia.addEventListener("change", function () {

        distrito.innerHTML = '<option value="">Selecione</option>';
        bairro.innerHTML   = '<option value="">Selecione</option>';

        distrito.disabled = true;
        bairro.disabled   = true;

        const provinciaId = this.value;

        if (!provinciaId) return;

        fetch(`/api/distritos/${provinciaId}`)
            .then(r => {
                if (!r.ok) throw new Error("Erro ao carregar distritos");
                return r.json();
            })
            .then(data => {

                if (data.length === 0) {
                    distrito.innerHTML = '<option value="">Sem distritos</option>';
                    return;
                }

                data.forEach(d => {
                    distrito.innerHTML += `<option value="${d.id}">${d.nome}</option>`;
                });

                distrito.disabled = false;
            })
            .catch(err => console.error(err));
    });

    // ================= BAIRROS =================
    distrito.addEventListener("change", function () {

        bairro.innerHTML = '<option value="">Selecione</option>';
        bairro.disabled  = true;

        const distritoId = this.value;

        if (!distritoId) return;

        fetch(`/api/bairros/${distritoId}`)
            .then(r => {
                if (!r.ok) throw new Error("Erro ao carregar bairros");
                return r.json();
            })
            .then(data => {

                if (data.length === 0) {
                    bairro.innerHTML = '<option value="">Sem bairros</option>';
                    return;
                }

                data.forEach(b => {
                    bairro.innerHTML += `<option value="${b.id}">${b.nome}</option>`;
                });

                bairro.disabled = false;
            })
            .catch(err => console.error(err));
    });

});