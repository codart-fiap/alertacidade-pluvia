package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.LeituraRequest;
import br.com.fiap.pluvia_api.dto.LeituraResponse;
import br.com.fiap.pluvia_api.dto.SensorResponse;
import br.com.fiap.pluvia_api.exception.RecursoNaoEncontradoException;
import br.com.fiap.pluvia_api.model.Alerta;
import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.LeituraSensor;
import br.com.fiap.pluvia_api.model.NivelRisco;
import br.com.fiap.pluvia_api.model.Sensor;
import br.com.fiap.pluvia_api.repository.AlertaRepository;
import br.com.fiap.pluvia_api.repository.AreaRiscoRepository;
import br.com.fiap.pluvia_api.repository.LeituraSensorRepository;
import br.com.fiap.pluvia_api.repository.SensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Recebe as leituras dos sensores (reais ou simulados). Quando o valor atinge o limite do sensor,
 * gera um alerta e ativa a area de risco no mapa.
 */
@Service
public class SensorService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final SensorRepository sensorRepository;
    private final LeituraSensorRepository leituraRepository;
    private final AlertaRepository alertaRepository;
    private final AreaRiscoRepository areaRiscoRepository;

    public SensorService(SensorRepository sensorRepository, LeituraSensorRepository leituraRepository,
                         AlertaRepository alertaRepository, AreaRiscoRepository areaRiscoRepository) {
        this.sensorRepository = sensorRepository;
        this.leituraRepository = leituraRepository;
        this.alertaRepository = alertaRepository;
        this.areaRiscoRepository = areaRiscoRepository;
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> listar() {
        return sensorRepository.findAll().stream()
                .map(sensor -> SensorResponse.from(sensor,
                        leituraRepository.findFirstBySensor_IdOrderByDataHoraDesc(sensor.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public LeituraResponse registrarLeitura(Long sensorId, LeituraRequest req) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sensor " + sensorId + " não encontrado"));

        LeituraSensor leitura = new LeituraSensor();
        leitura.setSensor(sensor);
        leitura.setValor(req.valor());
        leitura.setDataHora(LocalDateTime.now());
        leitura = leituraRepository.save(leitura);

        boolean alertaGerado = false;
        if (req.valor() >= sensor.getLimiteAlerta()) {
            gerarAlerta(sensor, leitura);
            alertaGerado = true;
        }
        return new LeituraResponse(leitura.getId(), sensor.getId(), leitura.getValor(),
                leitura.getDataHora(), alertaGerado);
    }

    private void gerarAlerta(Sensor sensor, LeituraSensor leitura) {
        NivelRisco nivel = calcularNivel(leitura.getValor(), sensor.getLimiteAlerta());
        AreaRisco area = sensor.getAreaRisco();

        Alerta alerta = new Alerta();
        alerta.setLeitura(leitura);
        alerta.setNivel(nivel);
        alerta.setMensagem(String.format(PT_BR,
                "Risco de enchente (%s) em %s: %s em %.1f %s (limite %.1f %s)",
                nivel, area.getNome(), sensor.getTipo().getDescricao(),
                leitura.getValor(), sensor.getTipo().getUnidade(),
                sensor.getLimiteAlerta(), sensor.getTipo().getUnidade()));
        alertaRepository.save(alerta);

        // O sensor acima do limite faz a area aparecer (ou ficar mais grave) no mapa do app.
        area.setNivelRisco(nivel);
        area.setAtiva(true);
        areaRiscoRepository.save(area);
    }

    /** Quanto maior a razao valor/limite, maior o nivel: ate 1,2x BAIXO; ate 1,5x MEDIO; acima ALTO. */
    static NivelRisco calcularNivel(double valor, double limite) {
        if (limite <= 0) {
            return NivelRisco.ALTO;
        }
        double razao = valor / limite;
        if (razao >= 1.5) {
            return NivelRisco.ALTO;
        }
        if (razao >= 1.2) {
            return NivelRisco.MEDIO;
        }
        return NivelRisco.BAIXO;
    }
}
