package br.helpdesk.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.helpdesk.helpdesk.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);
}