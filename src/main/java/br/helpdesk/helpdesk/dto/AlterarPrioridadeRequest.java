package br.helpdesk.helpdesk.dto;

import br.helpdesk.helpdesk.model.enums.Prioridade;

// Corpo do PATCH /chamados/{id}/prioridade
public record AlterarPrioridadeRequest(Prioridade prioridade) {
}
