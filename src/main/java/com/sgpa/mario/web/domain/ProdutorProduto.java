package com.sgpa.mario.web.domain;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "produtor_has_produto")
public class ProdutorProduto extends AbstractEntity<Long> {

	@ManyToOne
    @JoinColumn(name = "Produtor_idProdutor")
    private Produtor produtor;

    @ManyToOne
    @JoinColumn(name = "Produto_idProduto")
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "Distrito_idDistrito")
    private Distrito distrito;

    private String local;

	public String getLocal() {
		return local;
	}

	public void setLocal(String local) {
		this.local = local;
	}

   
}