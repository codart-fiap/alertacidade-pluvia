package br.com.fiap.pluvia_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.fiap.pluvia_api.dto.PontoSeguroRequest;
import br.com.fiap.pluvia_api.dto.PontoSeguroResponse;
import br.com.fiap.pluvia_api.service.PontoSeguroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pontos-seguros")
public class PontoSeguroController {

    private final PontoSeguroService service;

    public PontoSeguroController(PontoSeguroService service) {
        this.service = service;
    }

    @Operation(summary = "Lista os pontos seguros cadastrados")
    @GetMapping
    public List<PontoSeguroResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Cadastra um ponto seguro (ADMIN)")
    @PostMapping
    public ResponseEntity<PontoSeguroResponse> criar(@Valid @RequestBody PontoSeguroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(req));
    }
}
