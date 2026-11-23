package br.helpdesk.helpdesk.dto;

import java.time.LocalDateTime;

import br.helpdesk.helpdesk.model.HistoricoChamado;

// Um evento do histórico: data/hora e descrição
public record HistoricoResponse(LocalDateTime dataHora, String descricao) {

    public static HistoricoResponse de(HistoricoChamado historico) {
        return new HistoricoResponse(historico.getDataHora(), historico.getDescricao());
    }
}
