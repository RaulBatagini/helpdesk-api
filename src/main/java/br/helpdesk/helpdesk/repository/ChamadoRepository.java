package br.helpdesk.helpdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.helpdesk.helpdesk.model.Chamado;
import br.helpdesk.helpdesk.model.enums.StatusChamado;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    List<Chamado> findByStatus(StatusChamado status);
}