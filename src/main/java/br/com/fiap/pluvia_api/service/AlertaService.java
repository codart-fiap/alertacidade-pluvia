package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.AlertaResponse;
import br.com.fiap.pluvia_api.repository.AlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository repository;

    public AlertaService(AlertaRepository repository) {
        this.repository = repository;
    }

    /** Os 50 alertas mais recentes, do mais novo para o mais antigo. */
    @Transactional(readOnly = true)
    public List<AlertaResponse> listarRecentes() {
        return repository.findTop50ByOrderByLeituraDataHoraDesc().stream().map(AlertaResponse::from).toList();
    }
}
