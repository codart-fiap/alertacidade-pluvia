package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.RecomendacaoResponse;
import br.com.fiap.pluvia_api.exception.RequisicaoInvalidaException;
import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.NivelRisco;
import br.com.fiap.pluvia_api.model.PontoSeguro;
import br.com.fiap.pluvia_api.model.Situacao;
import br.com.fiap.pluvia_api.repository.AreaRiscoRepository;
import br.com.fiap.pluvia_api.repository.PontoSeguroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {

    // Area de risco de 700 m de raio centrada em (-23.5160, -46.6280)
    private static final double AREA_LAT = -23.5160;
    private static final double AREA_LNG = -46.6280;

    @Mock
    AreaRiscoRepository areaRepository;
    @Mock
    PontoSeguroRepository pontoRepository;

    @InjectMocks
    RecomendacaoService service;

    private AreaRisco area;

    @BeforeEach
    void preparar() {
        area = new AreaRisco();
        area.setId(1L);
        area.setNome("Área A");
        area.setLatitude(AREA_LAT);
        area.setLongitude(AREA_LNG);
        area.setRaioMetros(700);
        area.setNivelRisco(NivelRisco.ALTO);
        area.setAtiva(true);
    }

    @Test
    void dentroDaAreaIndicaPontoSeguroForaDelaMesmoSeHouverOutroMaisPerto() {
        PontoSeguro dentroDaArea = ponto(1L, "Ponto dentro da área", AREA_LAT - 0.0005, AREA_LNG - 0.0005);
        PontoSeguro foraDaArea = ponto(2L, "Escola", -23.5050, -46.6000);
        when(areaRepository.findByAtivaTrue()).thenReturn(List.of(area));
        when(pontoRepository.findAll()).thenReturn(List.of(dentroDaArea, foraDaArea));

        RecomendacaoResponse resposta = service.recomendar(AREA_LAT, AREA_LNG);

        assertEquals(Situacao.DENTRO_DA_AREA_DE_RISCO, resposta.situacao());
        assertEquals(NivelRisco.ALTO, resposta.nivelRisco());
        assertEquals(0L, resposta.distanciaAteBordaMetros());
        assertNotNull(resposta.pontoSeguro());
        assertEquals("Escola", resposta.pontoSeguro().nome());
    }

    @Test
    void pertoDaBordaAvisaQueEstaProximo() {
        // ~0,0072 grau de latitude = ~800 m do centro, ou seja ~100 m alem da borda de 700 m
        double lat = AREA_LAT + 0.0072;
        when(areaRepository.findByAtivaTrue()).thenReturn(List.of(area));
        when(pontoRepository.findAll()).thenReturn(List.of(ponto(2L, "Escola", -23.5050, -46.6000)));

        RecomendacaoResponse resposta = service.recomendar(lat, AREA_LNG);

        assertEquals(Situacao.PROXIMO_DA_AREA_DE_RISCO, resposta.situacao());
        assertNotNull(resposta.pontoSeguro());
    }

    @Test
    void longeDaAreaNaoPrecisaDePontoSeguro() {
        when(areaRepository.findByAtivaTrue()).thenReturn(List.of(area));

        RecomendacaoResponse resposta = service.recomendar(-23.6500, -46.7500);

        assertEquals(Situacao.FORA_DA_AREA_DE_RISCO, resposta.situacao());
        assertNull(resposta.pontoSeguro());
    }

    @Test
    void semAreasAtivasInformaQueNaoHaRisco() {
        when(areaRepository.findByAtivaTrue()).thenReturn(List.of());

        RecomendacaoResponse resposta = service.recomendar(AREA_LAT, AREA_LNG);

        assertEquals(Situacao.SEM_AREAS_ATIVAS, resposta.situacao());
        assertNull(resposta.areaRisco());
    }

    @Test
    void semPontoSeguroDisponivelOrientaLigarParaDefesaCivil() {
        when(areaRepository.findByAtivaTrue()).thenReturn(List.of(area));
        when(pontoRepository.findAll()).thenReturn(List.of());

        RecomendacaoResponse resposta = service.recomendar(AREA_LAT, AREA_LNG);

        assertEquals(Situacao.DENTRO_DA_AREA_DE_RISCO, resposta.situacao());
        assertNull(resposta.pontoSeguro());
        assertEquals(true, resposta.mensagem().contains("199"));
    }

    @Test
    void coordenadasForaDoIntervaloSaoRejeitadas() {
        assertThrows(RequisicaoInvalidaException.class, () -> service.recomendar(95, -46.6));
        assertThrows(RequisicaoInvalidaException.class, () -> service.recomendar(-23.5, 200));
    }

    private PontoSeguro ponto(Long id, String nome, double lat, double lng) {
        PontoSeguro p = new PontoSeguro();
        p.setId(id);
        p.setNome(nome);
        p.setLatitude(lat);
        p.setLongitude(lng);
        return p;
    }
}
