package br.com.fiap.pluvia_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.fiap.pluvia_api.dto.AlertaResponse;
import br.com.fiap.pluvia_api.service.AlertaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @Operation(summary = "Lista os 50 alertas mais recentes (o app consulta periodicamente)")
    @GetMapping
    public List<AlertaResponse> listar() {
        return service.listarRecentes();
    }
}
