package br.helpdesk.helpdesk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.helpdesk.helpdesk.dto.UsuarioRequest;
import br.helpdesk.helpdesk.dto.UsuarioResponse;
import br.helpdesk.helpdesk.service.UsuarioService;

//Indica que a classe é um controller
@RestController
//Define o caminho base dos endpoints
@RequestMapping("/usuarios")
public class UsuarioController {


    private final UsuarioService usuarioService;

    //Construtor do service
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    //Metodo post
    @PostMapping
    // recebe a requisição de cadastro de usuario em JSON e convert para UsuarioRequest
    // e depois retorna uma reposta UsuarioResponse
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.cadastrar(request));
    }
}