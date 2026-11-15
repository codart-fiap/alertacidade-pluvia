package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.NivelRisco;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AreaRiscoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @Positive Double raioMetros,
        @NotNull NivelRisco nivelRisco,
        Boolean ativa) {
}
