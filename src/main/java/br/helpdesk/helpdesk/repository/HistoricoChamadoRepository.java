package br.helpdesk.helpdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.helpdesk.helpdesk.model.HistoricoChamado;

public interface HistoricoChamadoRepository extends JpaRepository<HistoricoChamado, Long> {

    List<HistoricoChamado> findByChamadoIdOrderByDataHoraAsc(Long chamadoId);
}