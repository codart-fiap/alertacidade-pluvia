package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.NivelRisco;
import br.com.fiap.pluvia_api.model.Situacao;

public record RecomendacaoResponse(
        Situacao situacao,
        NivelRisco nivelRisco,
        String areaRisco,
        Long distanciaAteBordaMetros,
        PontoSeguroProximo pontoSeguro,
        String mensagem) {
}
