package br.helpdesk.helpdesk.dto;

import java.time.LocalDateTime;

import br.helpdesk.helpdesk.model.Chamado;
import br.helpdesk.helpdesk.model.enums.Categoria;
import br.helpdesk.helpdesk.model.enums.Prioridade;
import br.helpdesk.helpdesk.model.enums.StatusChamado;

// Define quais informações o sistema vai devolver
// após a abertura do chamado.
public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        Categoria categoria,
        Prioridade prioridade,
        StatusChamado status,
        Long solicitanteId,
        LocalDateTime dataAbertura
) {

    //de cima define o de baixo retorna
    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getCategoria(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                chamado.getSolicitante().getId(),
                chamado.getDataAbertura()
        );
    }
}