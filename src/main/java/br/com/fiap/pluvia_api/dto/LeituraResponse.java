package br.com.fiap.pluvia_api.dto;

import java.time.LocalDateTime;

public record LeituraResponse(
        Long leituraId,
        Long sensorId,
        double valor,
        LocalDateTime dataHora,
        boolean alertaGerado) {
}
