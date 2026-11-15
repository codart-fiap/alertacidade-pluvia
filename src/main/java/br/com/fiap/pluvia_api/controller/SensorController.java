package br.com.fiap.pluvia_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.fiap.pluvia_api.dto.LeituraRequest;
import br.com.fiap.pluvia_api.dto.LeituraResponse;
import br.com.fiap.pluvia_api.dto.SensorResponse;
import br.com.fiap.pluvia_api.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sensores")
public class SensorController {

    private final SensorService service;

    public SensorController(SensorService service) {
        this.service = service;
    }

    @Operation(summary = "Lista os sensores cadastrados")
    @GetMapping
    public List<SensorResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Registra uma leitura do sensor; acima do limite gera alerta (ADMIN ou dispositivo com X-Device-Key)")
    @PostMapping("/{id}/leituras")
    public ResponseEntity<LeituraResponse> registrarLeitura(@PathVariable Long id,
                                                            @Valid @RequestBody LeituraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarLeitura(id, req));
    }
}
