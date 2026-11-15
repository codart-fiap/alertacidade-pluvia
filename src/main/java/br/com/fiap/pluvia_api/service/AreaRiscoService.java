package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.AreaRiscoRequest;
import br.com.fiap.pluvia_api.dto.AreaRiscoResponse;
import br.com.fiap.pluvia_api.exception.RecursoNaoEncontradoException;
import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.repository.AreaRiscoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AreaRiscoService {

    private final AreaRiscoRepository repository;

    public AreaRiscoService(AreaRiscoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AreaRiscoResponse> listar() {
        return repository.findAll().stream().map(AreaRiscoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AreaRiscoResponse buscar(Long id) {
        return AreaRiscoResponse.from(buscarEntidade(id));
    }

    @Transactional
    public AreaRiscoResponse criar(AreaRiscoRequest req) {
        AreaRisco area = new AreaRisco();
        copiar(req, area);
        area.setAtiva(req.ativa() == null || req.ativa());
        return AreaRiscoResponse.from(repository.save(area));
    }

    @Transactional
    public AreaRiscoResponse atualizar(Long id, AreaRiscoRequest req) {
        AreaRisco area = buscarEntidade(id);
        copiar(req, area);
        if (req.ativa() != null) {
            area.setAtiva(req.ativa());
        }
        return AreaRiscoResponse.from(repository.save(area));
    }

    /** Falha com 409 (tratado no GlobalExceptionHandler) se houver sensores ou alertas ligados a area. */
    @Transactional
    public void excluir(Long id) {
        AreaRisco area = buscarEntidade(id);
        repository.delete(area);
        repository.flush();
    }

    private AreaRisco buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Área de risco " + id + " não encontrada"));
    }

    private void copiar(AreaRiscoRequest req, AreaRisco area) {
        area.setNome(req.nome().trim());
        area.setLatitude(req.latitude());
        area.setLongitude(req.longitude());
        area.setRaioMetros(req.raioMetros());
        area.setNivelRisco(req.nivelRisco());
    }
}
