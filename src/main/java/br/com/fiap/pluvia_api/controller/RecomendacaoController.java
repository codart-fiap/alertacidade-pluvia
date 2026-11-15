package br.com.fiap.pluvia_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.fiap.pluvia_api.dto.RecomendacaoResponse;
import br.com.fiap.pluvia_api.service.RecomendacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecomendacaoController {

    private final RecomendacaoService service;

    public RecomendacaoController(RecomendacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Relatório personalizado a partir da localização da pessoa (tela 'Minha localização')")
    @GetMapping("/recomendacao")
    public RecomendacaoResponse recomendar(@RequestParam double lat, @RequestParam double lng) {
        return service.recomendar(lat, lng);
    }
}
