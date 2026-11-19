package br.helpdesk.helpdesk.service;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Optional;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import br.helpdesk.helpdesk.dto.ChamadoRequest;
import br.helpdesk.helpdesk.dto.ChamadoResponse;
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
}