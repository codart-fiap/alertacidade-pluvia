package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.Alerta;
import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.LeituraSensor;
import br.com.fiap.pluvia_api.model.NivelRisco;

import java.time.LocalDateTime;

public record AlertaResponse(
        Long id,
        Long areaRiscoId,
        String areaRiscoNome,
        NivelRisco nivel,
        String mensagem,
        double valorLeitura,
        LocalDateTime dataHora) {

    /** Area e horario vem da leitura que disparou o alerta (alerta -> leitura -> sensor -> area). */
    public static AlertaResponse from(Alerta a) {
        LeituraSensor leitura = a.getLeitura();
        AreaRisco area = leitura.getSensor().getAreaRisco();
        return new AlertaResponse(a.getId(), area.getId(), area.getNome(),
                a.getNivel(), a.getMensagem(), leitura.getValor(), leitura.getDataHora());
    }
}
