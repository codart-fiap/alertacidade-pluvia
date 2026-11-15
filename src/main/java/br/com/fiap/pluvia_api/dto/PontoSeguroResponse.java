package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.PontoSeguro;

public record PontoSeguroResponse(Long id, String nome, double latitude, double longitude) {

    public static PontoSeguroResponse from(PontoSeguro p) {
        return new PontoSeguroResponse(p.getId(), p.getNome(), p.getLatitude(), p.getLongitude());
    }
}
