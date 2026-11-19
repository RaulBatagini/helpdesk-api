package br.helpdesk.helpdesk.dto;

import br.helpdesk.helpdesk.model.enums.Perfil;

//DTO recebe os dados enviados pelo usuario para nao expor a entidade USUARIO
public record UsuarioRequest(String nome, String email, Perfil perfil) {
}
