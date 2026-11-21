package br.helpdesk.helpdesk.dto;

import br.helpdesk.helpdesk.model.enums.StatusChamado;

// Corpo do PATCH /chamados/{id}/status
public record AtualizarStatusRequest(StatusChamado status) {
}
