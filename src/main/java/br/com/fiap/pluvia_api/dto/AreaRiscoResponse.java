package br.com.fiap.pluvia_api.dto;

import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.NivelRisco;

public record AreaRiscoResponse(
        Long id,
        String nome,
        double latitude,
        double longitude,
        double raioMetros,
        NivelRisco nivelRisco,
        boolean ativa) {

    public static AreaRiscoResponse from(AreaRisco a) {
        return new AreaRiscoResponse(a.getId(), a.getNome(), a.getLatitude(), a.getLongitude(),
                a.getRaioMetros(), a.getNivelRisco(), a.isAtiva());
    }
}
