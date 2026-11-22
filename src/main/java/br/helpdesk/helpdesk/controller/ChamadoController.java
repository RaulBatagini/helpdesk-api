
package br.helpdesk.helpdesk.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.helpdesk.helpdesk.dto.AtribuirTecnicoRequest;
import br.helpdesk.helpdesk.dto.AtualizarStatusRequest;
import br.helpdesk.helpdesk.dto.ChamadoRequest;
import br.helpdesk.helpdesk.dto.ChamadoResponse;
import br.helpdesk.helpdesk.dto.HistoricoResponse;
import br.helpdesk.helpdesk.model.enums.StatusChamado;
import br.helpdesk.helpdesk.service.ChamadoService;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrirChamado(
            @RequestBody ChamadoRequest request) {

        ChamadoResponse response = chamadoService.abrirChamado(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ChamadoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody AtualizarStatusRequest request) {

        return ResponseEntity.ok(chamadoService.atualizarStatus(id, request.status()));
    }

    @GetMapping
    public ResponseEntity<List<ChamadoResponse>> listarChamados(
            @RequestParam(required = false) StatusChamado status) {

        return ResponseEntity.ok(chamadoService.listarChamados(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoResponse> buscarChamado(@PathVariable Long id) {
        return ResponseEntity.ok(chamadoService.buscarChamado(id));
    }

    @PutMapping("/{id}/atribuicao")
    public ResponseEntity<ChamadoResponse> atribuirTecnico(
            @PathVariable Long id,
            @RequestBody AtribuirTecnicoRequest request) {

        return ResponseEntity.ok(chamadoService.atribuirTecnico(id, request.tecnicoId()));
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoResponse>> consultarHistorico(@PathVariable Long id) {
        return ResponseEntity.ok(chamadoService.consultarHistorico(id));
    }
}
