package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.PontoSeguroRequest;
import br.com.fiap.pluvia_api.dto.PontoSeguroResponse;
import br.com.fiap.pluvia_api.model.PontoSeguro;
import br.com.fiap.pluvia_api.repository.PontoSeguroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PontoSeguroService {

    private final PontoSeguroRepository repository;

    public PontoSeguroService(PontoSeguroRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PontoSeguroResponse> listar() {
        return repository.findAll().stream().map(PontoSeguroResponse::from).toList();
    }

    @Transactional
    public PontoSeguroResponse criar(PontoSeguroRequest req) {
        PontoSeguro ponto = new PontoSeguro();
        ponto.setNome(req.nome().trim());
        ponto.setLatitude(req.latitude());
        ponto.setLongitude(req.longitude());
        return PontoSeguroResponse.from(repository.save(ponto));
    }
}
