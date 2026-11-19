package br.helpdesk.helpdesk.dto;

import br.helpdesk.helpdesk.model.enums.Categoria;
import br.helpdesk.helpdesk.model.enums.Prioridade;

// DTO recebe os dados enviados pelo usuário para abrir um chamado,
// sem expor diretamente a entidade Chamado.
public record ChamadoRequest(
        Long solicitanteId,
        String titulo,
        String descricao,
        Categoria categoria,
        Prioridade prioridade
) {}
