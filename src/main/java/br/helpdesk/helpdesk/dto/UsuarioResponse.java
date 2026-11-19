package br.helpdesk.helpdesk.dto;

import br.helpdesk.helpdesk.model.Usuario;
import br.helpdesk.helpdesk.model.enums.Perfil;

//Define quais informações o sistema vai devolver para quem fez a requisição no caso
// id, nome, email e o perfil
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(),
                usuario.getEmail(), usuario.getPerfil());
    }
}