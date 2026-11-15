package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.Sensor;
import br.com.fiap.pluvia_api.model.TipoSensor;
import br.com.fiap.pluvia_api.model.LeituraSensor;

import java.time.LocalDateTime;

public record SensorResponse(
        Long id,
        String nome,
        TipoSensor tipo,
        Long areaRiscoId,
        String areaRiscoNome,
        double limiteAlerta,
        Double ultimaLeitura,
        LocalDateTime dataHoraUltimaLeitura) {

    public static SensorResponse from(Sensor s, LeituraSensor leitura) {
        return new SensorResponse(s.getId(), s.getNome(), s.getTipo(),
                s.getAreaRisco().getId(), s.getAreaRisco().getNome(), s.getLimiteAlerta(),
                leitura == null ? null : leitura.getValor(),
                leitura == null ? null : leitura.getDataHora());
    }
}
