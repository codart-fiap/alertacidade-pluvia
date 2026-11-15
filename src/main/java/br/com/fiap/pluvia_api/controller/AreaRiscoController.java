package br.com.fiap.pluvia_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.fiap.pluvia_api.dto.AreaRiscoRequest;
import br.com.fiap.pluvia_api.dto.AreaRiscoResponse;
import br.com.fiap.pluvia_api.service.AreaRiscoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/areas-risco")
public class AreaRiscoController {

    private final AreaRiscoService service;

    public AreaRiscoController(AreaRiscoService service) {
        this.service = service;
    }

    @Operation(summary = "Lista as áreas de risco (o app desenha as ativas no mapa)")
    @GetMapping
    public List<AreaRiscoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca uma área de risco pelo id")
    @GetMapping("/{id}")
    public AreaRiscoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Cria uma área de risco (ADMIN)")
    @PostMapping
    public ResponseEntity<AreaRiscoResponse> criar(@Valid @RequestBody AreaRiscoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(req));
    }

    @Operation(summary = "Atualiza uma área de risco (ADMIN)")
    @PutMapping("/{id}")
    public AreaRiscoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AreaRiscoRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "Exclui uma área de risco sem sensores vinculados (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
