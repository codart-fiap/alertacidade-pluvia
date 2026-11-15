package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.PontoSeguroProximo;
import br.com.fiap.pluvia_api.dto.RecomendacaoResponse;
import br.com.fiap.pluvia_api.exception.RequisicaoInvalidaException;
import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.PontoSeguro;
import br.com.fiap.pluvia_api.model.Situacao;
import br.com.fiap.pluvia_api.repository.AreaRiscoRepository;
import br.com.fiap.pluvia_api.repository.PontoSeguroRepository;
import br.com.fiap.pluvia_api.util.GeoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gera o relatorio personalizado da tela "Minha localizacao": compara a posicao da pessoa com as
 * areas de risco ativas (por regras, sem IA) e indica o ponto seguro mais proximo.
 */
@Service
public class RecomendacaoService {

    /** Ate esta distancia da borda da area, a pessoa e avisada de que esta proxima do risco. */
    static final double DISTANCIA_PROXIMA_METROS = 500;

    private final AreaRiscoRepository areaRepository;
    private final PontoSeguroRepository pontoRepository;

    public RecomendacaoService(AreaRiscoRepository areaRepository, PontoSeguroRepository pontoRepository) {
        this.areaRepository = areaRepository;
        this.pontoRepository = pontoRepository;
    }

    @Transactional(readOnly = true)
    public RecomendacaoResponse recomendar(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new RequisicaoInvalidaException("Coordenadas inválidas: latitude -90 a 90, longitude -180 a 180");
        }

        List<AreaRisco> ativas = areaRepository.findByAtivaTrue();
        if (ativas.isEmpty()) {
            return new RecomendacaoResponse(Situacao.SEM_AREAS_ATIVAS, null, null, null, null,
                    "Nenhuma área de risco ativa no momento. Continue atento aos alertas.");
        }

        // Area mais relevante: se estiver dentro de alguma, a de maior nivel; senao, a mais proxima.
        AreaRisco alvo = null;
        double distBordaAlvo = 0;
        for (AreaRisco area : ativas) {
            double distBorda = distanciaAteBorda(lat, lng, area);
            if (alvo == null || melhor(area, distBorda, alvo, distBordaAlvo)) {
                alvo = area;
                distBordaAlvo = distBorda;
            }
        }

        long distanciaBorda = Math.round(Math.max(0, distBordaAlvo));

        if (distBordaAlvo > DISTANCIA_PROXIMA_METROS) {
            return new RecomendacaoResponse(Situacao.FORA_DA_AREA_DE_RISCO, alvo.getNivelRisco(),
                    alvo.getNome(), distanciaBorda, null,
                    String.format("Você está fora das áreas de risco ativas. A mais próxima é %s, a cerca de %d m.",
                            alvo.getNome(), distanciaBorda));
        }

        PontoSeguroProximo ponto = pontoSeguroMaisProximo(lat, lng, ativas);
        boolean dentro = distBordaAlvo <= 0;
        Situacao situacao = dentro ? Situacao.DENTRO_DA_AREA_DE_RISCO : Situacao.PROXIMO_DA_AREA_DE_RISCO;

        String abertura = dentro
                ? String.format("Você está dentro da área de risco %s (nível %s). Saia do local imediatamente",
                alvo.getNome(), alvo.getNivelRisco())
                : String.format("Você está a %d m da área de risco %s (nível %s). Afaste-se e evite entrar nela",
                distanciaBorda, alvo.getNome(), alvo.getNivelRisco());
        String mensagem = ponto != null
                ? String.format("%s e siga para %s, a cerca de %d m de você.", abertura, ponto.nome(), ponto.distanciaMetros())
                : abertura + ". Procure um local alto e afastado da área e ligue 199 (Defesa Civil).";

        return new RecomendacaoResponse(situacao, alvo.getNivelRisco(), alvo.getNome(),
                distanciaBorda, ponto, mensagem);
    }

    /** Distancia da pessoa ate a borda da area, em metros (negativa se estiver dentro). */
    private double distanciaAteBorda(double lat, double lng, AreaRisco area) {
        return GeoUtil.distanciaMetros(lat, lng, area.getLatitude(), area.getLongitude()) - area.getRaioMetros();
    }

    private boolean melhor(AreaRisco a, double distA, AreaRisco b, double distB) {
        boolean aDentro = distA <= 0;
        boolean bDentro = distB <= 0;
        if (aDentro && bDentro) {
            return a.getNivelRisco().compareTo(b.getNivelRisco()) > 0;
        }
        if (aDentro != bDentro) {
            return aDentro;
        }
        return distA < distB;
    }

    /** Ponto seguro mais proximo da pessoa que nao esteja dentro de nenhuma area de risco ativa. */
    private PontoSeguroProximo pontoSeguroMaisProximo(double lat, double lng, List<AreaRisco> ativas) {
        PontoSeguro melhor = null;
        double menor = Double.MAX_VALUE;
        for (PontoSeguro ponto : pontoRepository.findAll()) {
            boolean emAreaDeRisco = ativas.stream()
                    .anyMatch(a -> distanciaAteBorda(ponto.getLatitude(), ponto.getLongitude(), a) <= 0);
            if (emAreaDeRisco) {
                continue;
            }
            double dist = GeoUtil.distanciaMetros(lat, lng, ponto.getLatitude(), ponto.getLongitude());
            if (dist < menor) {
                menor = dist;
                melhor = ponto;
            }
        }
        if (melhor == null) {
            return null;
        }
        return new PontoSeguroProximo(melhor.getId(), melhor.getNome(),
                melhor.getLatitude(), melhor.getLongitude(), Math.round(menor));
    }
}
