
package br.helpdesk.helpdesk.service;

import br.helpdesk.helpdesk.dto.UsuarioRequest;
import br.helpdesk.helpdesk.exception.ConflitoException;
import br.helpdesk.helpdesk.repository.UsuarioRepository;
import br.helpdesk.helpdesk.model.enums.Perfil;

// AQUI IMPORTAMOS AS LIBS PARA TESTE: EASYMOCK, TESTNG E HAMCREST
import org.easymock.EasyMock;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.expectThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class UsuarioServiceTest {

    // DATA PROVIDER: FORNECE DIFERENTES DADOS PARA O TESTE
    @DataProvider(name = "dadosUsuarios")
    public Object[][] dadosUsuarios() {
        return new Object[][] {
                {"Ana", " ANA@Email.com ", Perfil.SOLICITANTE},
                {"Carlos", " CARLOS@EMAIL.COM ", Perfil.SOLICITANTE},
                {"Maria", " MARIA@EMAIL.COM ", Perfil.TECNICO}
        };
    }

    // @TEST INFORMA QUE ESSE METODO É UM TESTE AUTOMATIZADO
    // O DATAPROVIDER FAZ O TESTE EXECUTAR COM CADA CONJUNTO DE DADOS
    @Test(dataProvider = "dadosUsuarios")
    public void deveImpedirCadastroComEmailDuplicado(
            String nome,
            String emailDigitado,
            Perfil perfil) {

        // ARRANGE: PREPARAÇÃO DO CENÁRIO

        // DADOS DE ENTRADA RECEBIDOS DO DATAPROVIDER
        String emailEsperado = emailDigitado.trim().toLowerCase();

        UsuarioRepository repository =
                EasyMock.createMock(UsuarioRepository.class);

        UsuarioRequest request = new UsuarioRequest(
                nome,
                emailDigitado,
                perfil
        );

        // SIMULA QUE O EMAIL JÁ EXISTE NO BANCO
        EasyMock.expect(
                repository.existsByEmail(emailEsperado)
        ).andReturn(true);

        // ATIVA O COMPORTAMENTO DO MOCK
        EasyMock.replay(repository);

        // CRIA O SERVICE UTILIZANDO O REPOSITÓRIO FALSO
        UsuarioService service = new UsuarioService(repository);

        // ACT: EXECUTA O MÉTODO QUE ESTAMOS TESTANDO
        ConflitoException exception = expectThrows(
                ConflitoException.class,
                () -> service.cadastrar(request)
        );

        // ASSERT: VERIFICA SE A MENSAGEM CONTÉM O EMAIL NORMALIZADO
        assertThat(
                exception.getMessage(),
                containsString(emailEsperado)
        );

        // VERIFY: CONFIRMA QUE O MOCK TEVE AS INTERAÇÕES ESPERADAS
        // UMA CHAMADA INESPERADA A SAVE TAMBÉM FARÁ O TESTE FALHAR
        EasyMock.verify(repository);
    }
}
