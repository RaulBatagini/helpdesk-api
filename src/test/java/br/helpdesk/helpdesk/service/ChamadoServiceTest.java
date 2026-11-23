package br.helpdesk.helpdesk.service;

import org.easymock.IMocksControl;
import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.expectThrows;

import java.util.List;
import java.util.Optional;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import br.helpdesk.helpdesk.dto.ChamadoRequest;
import br.helpdesk.helpdesk.dto.ChamadoResponse;
import br.helpdesk.helpdesk.dto.HistoricoResponse;
import br.helpdesk.helpdesk.exception.RecursoNaoEncontradoException;
import br.helpdesk.helpdesk.exception.RegraNegocioException;
import br.helpdesk.helpdesk.model.Chamado;
import br.helpdesk.helpdesk.model.HistoricoChamado;
import br.helpdesk.helpdesk.model.Usuario;
import br.helpdesk.helpdesk.model.enums.Categoria;
import br.helpdesk.helpdesk.model.enums.Perfil;
import br.helpdesk.helpdesk.model.enums.Prioridade;
import br.helpdesk.helpdesk.model.enums.StatusChamado;
import br.helpdesk.helpdesk.repository.ChamadoRepository;
import br.helpdesk.helpdesk.repository.HistoricoChamadoRepository;
import br.helpdesk.helpdesk.repository.UsuarioRepository;

public class ChamadoServiceTest extends EasyMockSupport {

    private ChamadoRepository chamadoRepository;
    private HistoricoChamadoRepository historicoRepository;
    private UsuarioRepository usuarioRepository;
    private ChamadoService chamadoService;

    @BeforeMethod
    public void setUp() {
        // Um único controle estrito para os 3 mocks a ordem passa a valer entre eles
        IMocksControl control = createStrictControl();
        chamadoRepository = control.createMock(ChamadoRepository.class);
        historicoRepository = control.createMock(HistoricoChamadoRepository.class);
        usuarioRepository = control.createMock(UsuarioRepository.class);

        chamadoService = new ChamadoService(chamadoRepository, historicoRepository, usuarioRepository);
    }

    @AfterMethod
    public void tearDown() {
        resetAll();
    }

    @Test
    public void deveAbrirChamadoComPrioridadeNulaComSucesso() {
        // Arrange
        ChamadoRequest request = new ChamadoRequest(
                1L,
                "Sem acesso à internet no setor",
                "Falha na rede",
                Categoria.REDE,
                null

        );

        Usuario solicitanteMock = new Usuario("João Silva", "joao@email.com", Perfil.SOLICITANTE);

        // 1º Passo esperado: Buscar usuário
        expect(usuarioRepository.findById(1L)).andReturn(Optional.of(solicitanteMock));

        // 2º Passo esperado: Salvar o chamado (utilizando Capture)
        Capture<Chamado> chamadoCaptor = newCapture();
        expect(chamadoRepository.save(capture(chamadoCaptor))).andAnswer(() -> {
            return chamadoCaptor.getValue();
        });

        // 3º Passo esperado: Salvar o histórico (utilizando Capture)
        Capture<HistoricoChamado> historicoCaptor = newCapture();
        expect(historicoRepository.save(capture(historicoCaptor))).andReturn(null);

        // Ativa os mocks em modo estrito
        replayAll();

        // Act
        ChamadoResponse response = chamadoService.abrirChamado(request);

        // Assert (Hamcrest)
        assertThat(response, notNullValue());
        assertThat(response.prioridade(), is(Prioridade.MEDIA));
        assertThat(response.status(), is(StatusChamado.ABERTO));

        Chamado chamadoCapturado = chamadoCaptor.getValue();
        assertThat(chamadoCapturado.getPrioridade(), is(Prioridade.MEDIA));
        assertThat(chamadoCapturado.getStatus(), is(StatusChamado.ABERTO));
        assertThat(chamadoCapturado.getSolicitante(), is(solicitanteMock));

        HistoricoChamado historicoCapturado = historicoCaptor.getValue();
        assertThat(historicoCapturado.getChamado(), is(chamadoCapturado));
        assertThat(historicoCapturado.getDescricao(), is("Chamado aberto"));

        // Valida se todas as expectativas ocorreram na ordem exata definida acima
        verifyAll();
    }

    @Test
    public void deveLancarNaoEncontradoAoAbrirChamadoComSolicitanteInexistente() {
        expect(usuarioRepository.findById(99L)).andReturn(Optional.empty());
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class, () -> chamadoService.abrirChamado(
                new ChamadoRequest(99L, "Sem acesso", "Falha na rede", Categoria.REDE, null)));

        verifyAll(); // nenhum save esperado
    }

    @Test
    public void deveRejeitarAberturaDeChamadoPorUsuarioTecnico() {
        Usuario tecnico = new Usuario("Maria Souza", "maria@email.com", Perfil.TECNICO);
        expect(usuarioRepository.findById(1L)).andReturn(Optional.of(tecnico));
        replayAll();

        expectThrows(RegraNegocioException.class, () -> chamadoService.abrirChamado(
                new ChamadoRequest(1L, "Sem acesso", "Falha na rede", Categoria.REDE, null)));

        verifyAll();
    }

    // ---------- US004: atualizar status do chamado ----------

    private Chamado chamadoEm(StatusChamado status) {
        Usuario solicitante = new Usuario("João Silva", "joao@email.com", Perfil.SOLICITANTE);
        Chamado chamado = new Chamado("Sem acesso", "Falha na rede", Categoria.HARDWARE,
                Prioridade.MEDIA, solicitante);
        chamado.setStatus(status);
        return chamado;
    }

    // As 16 combinações origem/destino
    @DataProvider(name = "transicoes")
    public Object[][] transicoes() {
        StatusChamado[] todos = StatusChamado.values();
        Object[][] casos = new Object[todos.length * todos.length][];
        int i = 0;
        for (StatusChamado origem : todos) {
            for (StatusChamado destino : todos) {
                casos[i++] = new Object[] { origem, destino };
            }
        }
        return casos;
    }

    @Test(dataProvider = "transicoes")
    public void deveAplicarSomenteTransicoesPermitidas(StatusChamado origem, StatusChamado destino) {
        // Regra do card, escrita aqui de forma independente do código de produção
        boolean permitida = (origem == StatusChamado.EM_ATENDIMENTO && destino == StatusChamado.RESOLVIDO)
                || (origem == StatusChamado.RESOLVIDO && destino == StatusChamado.FECHADO);

        Chamado chamado = chamadoEm(origem);
        Capture<HistoricoChamado> historicoCaptor = newCapture();
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        if (permitida) {
            expect(chamadoRepository.save(chamado)).andReturn(chamado);
            expect(historicoRepository.save(capture(historicoCaptor))).andReturn(null);
        }
        replayAll();

        if (permitida) {
            ChamadoResponse response = chamadoService.atualizarStatus(10L, destino);

            assertThat(response.status(), is(destino));
            assertThat(historicoCaptor.getValue().getDescricao(),
                    is("Status alterado de " + origem + " para " + destino));
        } else {
            expectThrows(RegraNegocioException.class,
                    () -> chamadoService.atualizarStatus(10L, destino));

            assertThat(chamado.getStatus(), is(origem));
        }

        // Transição inválida: nenhum save esperado, então qualquer save quebra o teste
        verifyAll();
    }

    @Test
    public void naoDeveGravarNadaAoPularEtapaDeAbertoParaFechado() {
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamadoEm(StatusChamado.ABERTO)));
        replayAll();

        RegraNegocioException erro = expectThrows(RegraNegocioException.class,
                () -> chamadoService.atualizarStatus(10L, StatusChamado.FECHADO));

        assertThat(erro.getMessage(), allOf(containsString("ABERTO"), containsString("FECHADO")));
        verifyAll();
    }

    @Test
    public void deveRejeitarAlteracaoEmChamadoFechado() {
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamadoEm(StatusChamado.FECHADO)));
        replayAll();

        RegraNegocioException erro = expectThrows(RegraNegocioException.class,
                () -> chamadoService.atualizarStatus(10L, StatusChamado.RESOLVIDO));

        assertThat(erro.getMessage(), containsString("FECHADO não aceita alteração"));
        verifyAll();
    }

    @Test
    public void deveLancarNaoEncontradoQuandoChamadoNaoExiste() {
        expect(chamadoRepository.findById(99L)).andReturn(Optional.empty());
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class,
                () -> chamadoService.atualizarStatus(99L, StatusChamado.RESOLVIDO));

        verifyAll();
    }

    @Test
    public void deveRejeitarStatusNuloSemConsultarOBanco() {
        replayAll();

        expectThrows(RegraNegocioException.class, () -> chamadoService.atualizarStatus(10L, null));

        verifyAll();
    }

    // ---------- US007: consultar histórico do chamado ----------

    @Test
    public void deveLancarNaoEncontradoAoConsultarHistoricoDeChamadoInexistente() {
        expect(chamadoRepository.existsById(99L)).andReturn(false);
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class, () -> chamadoService.consultarHistorico(99L));

        // historicoRepository não tem expectativa: qualquer consulta a ele quebra o teste
        verifyAll();
    }

    @Test
    public void deveRetornarListaVaziaQuandoChamadoNaoTemHistorico() {
        expect(chamadoRepository.existsById(10L)).andReturn(true);
        expect(historicoRepository.findByChamadoIdOrderByDataHoraAsc(10L)).andReturn(List.of());
        replayAll();

        assertThat(chamadoService.consultarHistorico(10L), is(empty()));

        verifyAll();
    }

    @Test
    public void deveRetornarEventosNaOrdemCronologicaComDataEDescricao() {
        Chamado chamado = chamadoEm(StatusChamado.EM_ATENDIMENTO);
        // A consulta ordenada por data é a esperada pelo mock; o service não reordena
        expect(chamadoRepository.existsById(10L)).andReturn(true);
        expect(historicoRepository.findByChamadoIdOrderByDataHoraAsc(10L)).andReturn(List.of(
                new HistoricoChamado(chamado, "Chamado aberto"),
                new HistoricoChamado(chamado, "Status alterado de EM_ATENDIMENTO para RESOLVIDO")));
        replayAll();

        List<HistoricoResponse> eventos = chamadoService.consultarHistorico(10L);

        assertThat(eventos.stream().map(HistoricoResponse::descricao).toList(),
                is(List.of("Chamado aberto", "Status alterado de EM_ATENDIMENTO para RESOLVIDO")));
        assertThat(eventos.get(0).dataHora(), notNullValue());
        assertThat(eventos.get(1).dataHora(), notNullValue());
        verifyAll();
    }

    // ---------- US006: alterar prioridade ----------

    @Test
    public void deveAlterarPrioridadeEGravarHistoricoComAntigaENova() {
        Chamado chamado = chamadoEm(StatusChamado.ABERTO); // prioridade inicial: MEDIA
        Capture<HistoricoChamado> historicoCaptor = newCapture();

        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        expect(chamadoRepository.save(chamado)).andReturn(chamado);
        expect(historicoRepository.save(capture(historicoCaptor))).andReturn(null);
        replayAll();

        ChamadoResponse resposta = chamadoService.alterarPrioridade(10L, Prioridade.ALTA);

        assertThat(resposta.prioridade(), is(Prioridade.ALTA));
        assertThat(historicoCaptor.getValue().getDescricao(),
                is("Prioridade alterada de MEDIA para ALTA"));
        verifyAll();
    }

    @Test
    public void naoDeveGravarNadaQuandoPrioridadeNaoMuda() {
        Chamado chamado = chamadoEm(StatusChamado.ABERTO);
        chamado.setPrioridade(Prioridade.ALTA);

        // Só findById é esperado: qualquer save quebra o teste
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        replayAll();

        ChamadoResponse resposta = chamadoService.alterarPrioridade(10L, Prioridade.ALTA);

        assertThat(resposta.prioridade(), is(Prioridade.ALTA));
        verifyAll();
    }

    @DataProvider(name = "statusNaoFechados")
    public Object[][] statusNaoFechados() {
        return new Object[][] {
                { StatusChamado.ABERTO }, { StatusChamado.EM_ATENDIMENTO }, { StatusChamado.RESOLVIDO } };
    }

    @Test(dataProvider = "statusNaoFechados")
    public void devePermitirAlterarPrioridadeEmQualquerStatusExcetoFechado(StatusChamado status) {
        Chamado chamado = chamadoEm(status);
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        expect(chamadoRepository.save(chamado)).andReturn(chamado);
        expect(historicoRepository.save(anyObject(HistoricoChamado.class))).andReturn(null);
        replayAll();

        ChamadoResponse resposta = chamadoService.alterarPrioridade(10L, Prioridade.CRITICA);

        assertThat(resposta.prioridade(), is(Prioridade.CRITICA));
        verifyAll();
    }

    @Test
    public void deveRejeitarAlteracaoDePrioridadeEmChamadoFechado() {
        Chamado chamado = chamadoEm(StatusChamado.FECHADO);
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        replayAll();

        RegraNegocioException erro = expectThrows(RegraNegocioException.class,
                () -> chamadoService.alterarPrioridade(10L, Prioridade.ALTA));

        assertThat(erro.getMessage(), containsString("FECHADO não aceita alteração de prioridade"));
        assertThat(chamado.getPrioridade(), is(Prioridade.MEDIA));
        verifyAll(); // nenhum save esperado
    }

    @Test
    public void deveRejeitarPrioridadeNulaSemConsultarOBanco() {
        replayAll();

        expectThrows(RegraNegocioException.class, () -> chamadoService.alterarPrioridade(10L, null));

        verifyAll();
    }

    @Test
    public void deveLancarNaoEncontradoAoAlterarPrioridadeDeChamadoInexistente() {
        expect(chamadoRepository.findById(99L)).andReturn(Optional.empty());
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class,
                () -> chamadoService.alterarPrioridade(99L, Prioridade.ALTA));

        verifyAll();
    }

    // ---------- US005: consultar chamados ----------

    @Test
    public void deveListarApenasChamadosDoStatusFiltrado() {
        // Só findByStatus é esperado: se o service chamar findAll, o mock estrito quebra o teste
        expect(chamadoRepository.findByStatus(StatusChamado.EM_ATENDIMENTO)).andReturn(List.of(
                chamadoEm(StatusChamado.EM_ATENDIMENTO),
                chamadoEm(StatusChamado.EM_ATENDIMENTO)));
        replayAll();

        List<ChamadoResponse> resultado = chamadoService.listarChamados(StatusChamado.EM_ATENDIMENTO);

        assertThat(resultado, hasSize(2));
        assertThat(resultado.stream().map(ChamadoResponse::status).toList(),
                is(List.of(StatusChamado.EM_ATENDIMENTO, StatusChamado.EM_ATENDIMENTO)));
        verifyAll();
    }

    @Test
    public void deveListarTodosOsChamadosQuandoNaoHaFiltro() {
        expect(chamadoRepository.findAll()).andReturn(List.of(
                chamadoEm(StatusChamado.ABERTO),
                chamadoEm(StatusChamado.EM_ATENDIMENTO),
                chamadoEm(StatusChamado.FECHADO)));
        replayAll();

        assertThat(chamadoService.listarChamados(null), hasSize(3));

        verifyAll();
    }

    @Test
    public void deveBuscarChamadoPorId() {
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamadoEm(StatusChamado.RESOLVIDO)));
        replayAll();

        ChamadoResponse resposta = chamadoService.buscarChamado(10L);

        assertThat(resposta.status(), is(StatusChamado.RESOLVIDO));
        assertThat(resposta.titulo(), is("Sem acesso"));
        verifyAll();
    }

    @Test
    public void deveLancarNaoEncontradoAoBuscarIdInexistente() {
        expect(chamadoRepository.findById(99L)).andReturn(Optional.empty());
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class, () -> chamadoService.buscarChamado(99L));

        verifyAll();
    }

    // ---------- US003: atribuir técnico ao chamado ----------

    @Test
    public void deveAtribuirTecnicoMudarStatusEGravarHistorico() {
        Chamado chamado = chamadoEm(StatusChamado.ABERTO);
        Usuario tecnico = new Usuario("Maria Souza", "maria@email.com", Perfil.TECNICO);
        Capture<Chamado> chamadoCaptor = newCapture();
        Capture<HistoricoChamado> historicoCaptor = newCapture();

        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        expect(usuarioRepository.findById(5L)).andReturn(Optional.of(tecnico));
        expect(chamadoRepository.save(capture(chamadoCaptor))).andReturn(chamado);
        expect(historicoRepository.save(capture(historicoCaptor))).andReturn(null);
        replayAll();

        ChamadoResponse resposta = chamadoService.atribuirTecnico(10L, 5L);

        // Uma única atribuição altera técnico e status ao mesmo tempo
        assertThat(chamadoCaptor.getValue(), allOf(
                hasProperty("tecnico", is(tecnico)),
                hasProperty("status", is(StatusChamado.EM_ATENDIMENTO))));
        assertThat(resposta.status(), is(StatusChamado.EM_ATENDIMENTO));
        assertThat(historicoCaptor.getValue().getDescricao(), containsString("Maria Souza"));
        verifyAll();
    }

    @Test
    public void deveRejeitarAtribuicaoParaUsuarioSolicitante() {
        Usuario solicitante = new Usuario("João Silva", "joao@email.com", Perfil.SOLICITANTE);
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamadoEm(StatusChamado.ABERTO)));
        expect(usuarioRepository.findById(5L)).andReturn(Optional.of(solicitante));
        replayAll();

        RegraNegocioException erro = expectThrows(RegraNegocioException.class,
                () -> chamadoService.atribuirTecnico(10L, 5L));

        assertThat(erro.getMessage(), containsString("não é um técnico"));
        verifyAll(); // nenhum save esperado
    }

    @DataProvider(name = "statusNaoAbertos")
    public Object[][] statusNaoAbertos() {
        return new Object[][] {
                { StatusChamado.EM_ATENDIMENTO }, { StatusChamado.RESOLVIDO }, { StatusChamado.FECHADO } };
    }

    @Test(dataProvider = "statusNaoAbertos")
    public void deveRejeitarAtribuicaoQuandoChamadoNaoEstaAberto(StatusChamado status) {
        Chamado chamado = chamadoEm(status);
        Usuario tecnico = new Usuario("Maria Souza", "maria@email.com", Perfil.TECNICO);
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamado));
        expect(usuarioRepository.findById(5L)).andReturn(Optional.of(tecnico));
        replayAll();

        expectThrows(RegraNegocioException.class, () -> chamadoService.atribuirTecnico(10L, 5L));

        assertThat(chamado.getTecnico(), nullValue());
        assertThat(chamado.getStatus(), is(status));
        verifyAll(); // nenhum save esperado
    }

    @Test
    public void deveLancarNaoEncontradoAoAtribuirEmChamadoInexistente() {
        expect(chamadoRepository.findById(99L)).andReturn(Optional.empty());
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class, () -> chamadoService.atribuirTecnico(99L, 5L));

        verifyAll(); // usuarioRepository sem expectativa: o técnico nem é buscado
    }

    @Test
    public void deveLancarNaoEncontradoQuandoTecnicoNaoExiste() {
        expect(chamadoRepository.findById(10L)).andReturn(Optional.of(chamadoEm(StatusChamado.ABERTO)));
        expect(usuarioRepository.findById(5L)).andReturn(Optional.empty());
        replayAll();

        expectThrows(RecursoNaoEncontradoException.class, () -> chamadoService.atribuirTecnico(10L, 5L));

        verifyAll();
    }

    @Test
    public void deveRejeitarAtribuicaoSemIdDoTecnico() {
        replayAll();

        expectThrows(RegraNegocioException.class, () -> chamadoService.atribuirTecnico(10L, null));

        verifyAll(); // nenhum repositório é consultado
    }
}