package br.helpdesk.helpdesk.model.enums;

public enum StatusChamado {
    ABERTO, EM_ATENDIMENTO, RESOLVIDO, FECHADO;

    // Fluxo do PATCH /status: EM_ATENDIMENTO -> RESOLVIDO -> FECHADO.
    // ABERTO -> EM_ATENDIMENTO só acontece ao atribuir técnico (US003).
    public boolean podeIrPara(StatusChamado novo) {
        return switch (this) {
            case EM_ATENDIMENTO -> novo == RESOLVIDO;
            case RESOLVIDO -> novo == FECHADO;
            default -> false;
        };
    }
}
