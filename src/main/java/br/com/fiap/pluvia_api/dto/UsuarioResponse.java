package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.Perfil;
import br.com.fiap.pluvia_api.model.Usuario;

/** Nunca expoe a senha (nem o hash). */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
    }
}
