package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.Perfil;

public record TokenResponse(String token, String tipo, Perfil perfil) {
}
