package br.com.fiap.pluvia_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LeituraRequest(@NotNull @PositiveOrZero Double valor) {
}
