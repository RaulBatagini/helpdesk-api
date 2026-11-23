
package br.helpdesk.helpdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.helpdesk.helpdesk.dto.ChamadoRequest;
import br.helpdesk.helpdesk.dto.ChamadoResponse;
import br.helpdesk.helpdesk.dto.HistoricoResponse;
import br.helpdesk.helpdesk.exception.RegraNegocioException;
import br.helpdesk.helpdesk.model.Chamado;
import br.helpdesk.helpdesk.model.HistoricoChamado;
import br.helpdesk.helpdesk.model.Usuario;
import br.helpdesk.helpdesk.model.enums.Perfil;
import br.helpdesk.helpdesk.model.enums.Prioridade;
import br.helpdesk.helpdesk.model.enums.StatusChamado;
import br.helpdesk.helpdesk.repository.ChamadoRepository;
import br.helpdesk.helpdesk.repository.HistoricoChamadoRepository;
import br.helpdesk.helpdesk.repository.UsuarioRepository;
import br.helpdesk.helpdesk.exception.RecursoNaoEncontradoException;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final HistoricoChamadoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            HistoricoChamadoRepository historicoRepository,
            UsuarioRepository usuarioRepository) {

        this.chamadoRepository = chamadoRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    //Garante que a abertura e o registro do histórico façam parte da mesma transação.
    // Se o histórico falhar, o salvamento do chamado também será revertido.

    public ChamadoResponse abrirChamado(ChamadoRequest request) {

        // 1. Validar se a requisição foi informada
        if (request == null) {
            throw new RegraNegocioException("Os dados do chamado são obrigatórios");
        }

        // 2. Validar o título
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new RegraNegocioException("O título é obrigatório");
        }

        if (request.titulo().trim().length() > 150) {
            throw new RegraNegocioException("O título deve ter no máximo 150 caracteres");
        }

        // 3. Validar a descrição
        if (request.descricao() == null || request.descricao().isBlank()) {
            throw new RegraNegocioException("A descrição é obrigatória");
        }

        if (request.descricao().trim().length() > 1000) {
            throw new RegraNegocioException("A descrição deve ter no máximo 1000 caracteres");
        }

        // 4. Validar a categoria
        if (request.categoria() == null) {
            throw new RegraNegocioException("A categoria é obrigatória");
        }

        // 5. Buscar o solicitante
        if (request.solicitanteId() == null) {
            throw new RegraNegocioException("O ID do solicitante é obrigatório");
        }

        Usuario solicitante = usuarioRepository.findById(request.solicitanteId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Solicitante não encontrado"));

        // 6. Verificar se o usuário é um solicitante
        if (solicitante.getPerfil() != Perfil.SOLICITANTE) {
            throw new RegraNegocioException("O usuário informado não é um solicitante");
        }

        // 7. Definir prioridade padrão
        Prioridade prioridade = request.prioridade() == null
                ? Prioridade.MEDIA
                : request.prioridade();

        // 8. Criar o chamado
        Chamado chamado = new Chamado(
                request.titulo().trim(),
                request.descricao().trim(),
                request.categoria(),
                prioridade,
                solicitante
        );

        // 9. Salvar o chamado
        Chamado chamadoSalvo = chamadoRepository.save(chamado);

        // 10. Registrar a abertura no histórico
        HistoricoChamado historico = new HistoricoChamado(
                chamadoSalvo,
                "Chamado aberto"
        );

        historicoRepository.save(historico);

        // 11. Retornar os dados do chamado
        return ChamadoResponse.de(chamadoSalvo);
    }

    // Mudança de status e registro no histórico na mesma transação.
    @Transactional
    public ChamadoResponse atualizarStatus(Long id, StatusChamado novoStatus) {

        if (novoStatus == null) {
            throw new RegraNegocioException("O novo status é obrigatório");
        }

        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));

        StatusChamado statusAtual = chamado.getStatus();

        if (statusAtual == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Chamado FECHADO não aceita alteração de status");
        }

        if (!statusAtual.podeIrPara(novoStatus)) {
            throw new RegraNegocioException(
                    "Transição de status inválida: " + statusAtual + " → " + novoStatus);
        }

        chamado.setStatus(novoStatus);
        Chamado chamadoSalvo = chamadoRepository.save(chamado);

        historicoRepository.save(new HistoricoChamado(chamadoSalvo,
                "Status alterado de " + statusAtual + " para " + novoStatus));

        return ChamadoResponse.de(chamadoSalvo);
    }

    // Troca da prioridade e registro no histórico na mesma transação.
    @Transactional
    public ChamadoResponse alterarPrioridade(Long id, Prioridade novaPrioridade) {

        if (novaPrioridade == null) {
            throw new RegraNegocioException("A nova prioridade é obrigatória");
        }

        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Chamado FECHADO não aceita alteração de prioridade");
        }

        Prioridade prioridadeAtual = chamado.getPrioridade();

        // Mesma prioridade: nada muda, então nada é gravado
        if (prioridadeAtual == novaPrioridade) {
            return ChamadoResponse.de(chamado);
        }

        chamado.setPrioridade(novaPrioridade);
        Chamado chamadoSalvo = chamadoRepository.save(chamado);

        historicoRepository.save(new HistoricoChamado(chamadoSalvo,
                "Prioridade alterada de " + prioridadeAtual + " para " + novaPrioridade));

        return ChamadoResponse.de(chamadoSalvo);
    }

    // Eventos do chamado em ordem cronológica (mais antigo primeiro).
    @Transactional(readOnly = true)
    public List<HistoricoResponse> consultarHistorico(Long id) {

        if (!chamadoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Chamado não encontrado");
        }

        return historicoRepository.findByChamadoIdOrderByDataHoraAsc(id).stream()
                .map(HistoricoResponse::de)
                .toList();
    }

    // Sem filtro lista todos; com filtro, só os chamados daquele status.
    @Transactional(readOnly = true)
    public List<ChamadoResponse> listarChamados(StatusChamado status) {

        List<Chamado> chamados = status == null
                ? chamadoRepository.findAll()
                : chamadoRepository.findByStatus(status);

        return chamados.stream().map(ChamadoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscarChamado(Long id) {
        return chamadoRepository.findById(id)
                .map(ChamadoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));
    }

    // Vínculo do técnico, mudança de status e histórico na mesma transação.
    @Transactional
    public ChamadoResponse atribuirTecnico(Long chamadoId, Long tecnicoId) {

        if (tecnicoId == null) {
            throw new RegraNegocioException("O ID do técnico é obrigatório");
        }

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));

        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Técnico não encontrado"));

        if (tecnico.getPerfil() != Perfil.TECNICO) {
            throw new RegraNegocioException("O usuário informado não é um técnico");
        }

        // Cobre também o FECHADO: só ABERTO recebe atribuição
        if (chamado.getStatus() != StatusChamado.ABERTO) {
            throw new RegraNegocioException(
                    "Só chamados ABERTO podem receber atribuição (status atual: " + chamado.getStatus() + ")");
        }

        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        Chamado chamadoSalvo = chamadoRepository.save(chamado);

        historicoRepository.save(new HistoricoChamado(chamadoSalvo,
                "Chamado atribuído ao técnico " + tecnico.getNome()));

        return ChamadoResponse.de(chamadoSalvo);
    }
}
