package br.helpdesk.helpdesk.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.helpdesk.helpdesk.dto.UsuarioRequest;
import br.helpdesk.helpdesk.dto.UsuarioResponse;
import br.helpdesk.helpdesk.exception.ConflitoException;
import br.helpdesk.helpdesk.exception.RegraNegocioException;
import br.helpdesk.helpdesk.model.Usuario;
import br.helpdesk.helpdesk.repository.UsuarioRepository;

// Indica ao Spring que esta classe é responsavel pela logica de negocio.
@Service
public class UsuarioService {

    //padrão (Regex) para verificar se o e-mail possui um formato válido
    private static final Pattern EMAIL_VALIDO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioRepository usuarioRepository;

    //Construtor para o repositório
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Metodo responsável por cadastrar um novo usuario.
    // Recebe os dados através do UsuarioRequest e retorna um UsuarioResponse
    public UsuarioResponse cadastrar(UsuarioRequest request) {
        validarNome(request.nome());
        String email = normalizarEmail(request.email());
        validarEmail(email);
        validarPerfil(request);

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe um usuário cadastrado com o e-mail " + email);
        }

        //Cria o objeto usuario com os dados recebidos
        Usuario usuario = new Usuario(request.nome().trim(), email, request.perfil());
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome é obrigatório");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraNegocioException("O e-mail é obrigatório");
        }
        // Remove espaços das extremidades e transforma todas as letras em minusculas
        return email.trim().toLowerCase();
    }

    private void validarEmail(String email) {
        if (!EMAIL_VALIDO.matcher(email).matches()) {
            throw new RegraNegocioException("O e-mail informado é inválido");
        }
    }

    private void validarPerfil(UsuarioRequest request) {
        if (request.perfil() == null) {
            throw new RegraNegocioException("O perfil é obrigatório (SOLICITANTE ou TECNICO)");
        }
    }
}