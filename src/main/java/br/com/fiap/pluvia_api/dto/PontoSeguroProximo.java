package br.com.fiap.pluvia_api.dto;

public record PontoSeguroProximo(
        Long id,
        String nome,
        double latitude,
        double longitude,
        long distanciaMetros) {
}
