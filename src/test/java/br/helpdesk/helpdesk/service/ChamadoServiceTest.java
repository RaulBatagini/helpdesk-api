package br.helpdesk.helpdesk.service;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.expectThrows;

import java.util.Optional;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import br.helpdesk.helpdesk.dto.ChamadoRequest;
import br.helpdesk.helpdesk.dto.ChamadoResponse;
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
        // USO EXPLICITO DO createStrictMock para forçar a ordem exata das chamadas
        chamadoRepository = createStrictMock(ChamadoRepository.class);
        historicoRepository = createStrictMock(HistoricoChamadoRepository.class);
        usuarioRepository = createStrictMock(UsuarioRepository.class);

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
                Categoria.HARDWARE,
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
}