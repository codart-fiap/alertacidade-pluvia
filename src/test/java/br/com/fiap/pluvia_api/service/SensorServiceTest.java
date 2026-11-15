package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.LeituraRequest;
import br.com.fiap.pluvia_api.dto.LeituraResponse;
import br.com.fiap.pluvia_api.exception.RecursoNaoEncontradoException;
import br.com.fiap.pluvia_api.model.Alerta;
import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.LeituraSensor;
import br.com.fiap.pluvia_api.model.NivelRisco;
import br.com.fiap.pluvia_api.model.Sensor;
import br.com.fiap.pluvia_api.model.TipoSensor;
import br.com.fiap.pluvia_api.repository.AlertaRepository;
import br.com.fiap.pluvia_api.repository.AreaRiscoRepository;
import br.com.fiap.pluvia_api.repository.LeituraSensorRepository;
import br.com.fiap.pluvia_api.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock
    SensorRepository sensorRepository;
    @Mock
    LeituraSensorRepository leituraRepository;
    @Mock
    AlertaRepository alertaRepository;
    @Mock
    AreaRiscoRepository areaRiscoRepository;

    @InjectMocks
    SensorService service;

    private AreaRisco area;
    private Sensor sensor;

    @BeforeEach
    void preparar() {
        area = new AreaRisco();
        area.setId(1L);
        area.setNome("Área A");
        area.setNivelRisco(NivelRisco.BAIXO);
        area.setAtiva(false);

        sensor = new Sensor();
        sensor.setId(10L);
        sensor.setNome("Régua de nível");
        sensor.setTipo(TipoSensor.NIVEL_RIO);
        sensor.setAreaRisco(area);
        sensor.setLimiteAlerta(300);
    }

    @Test
    void leituraAbaixoDoLimiteNaoGeraAlerta() {
        when(sensorRepository.findById(10L)).thenReturn(Optional.of(sensor));
        when(leituraRepository.save(any(LeituraSensor.class))).thenAnswer(i -> i.getArgument(0));

        LeituraResponse resposta = service.registrarLeitura(10L, new LeituraRequest(120.0));

        assertFalse(resposta.alertaGerado());
        verify(alertaRepository, never()).save(any(Alerta.class));
        assertFalse(area.isAtiva());
    }

    @Test
    void leituraMuitoAcimaDoLimiteGeraAlertaAltoEAtivaArea() {
        when(sensorRepository.findById(10L)).thenReturn(Optional.of(sensor));
        when(leituraRepository.save(any(LeituraSensor.class))).thenAnswer(i -> i.getArgument(0));

        LeituraResponse resposta = service.registrarLeitura(10L, new LeituraRequest(480.0));

        assertTrue(resposta.alertaGerado());
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        assertEquals(NivelRisco.ALTO, captor.getValue().getNivel());
        assertEquals(area, captor.getValue().getLeitura().getSensor().getAreaRisco());
        assertTrue(area.isAtiva());
        assertEquals(NivelRisco.ALTO, area.getNivelRisco());
        verify(areaRiscoRepository).save(area);
    }

    @Test
    void leituraNoLimiteJaGeraAlerta() {
        when(sensorRepository.findById(10L)).thenReturn(Optional.of(sensor));
        when(leituraRepository.save(any(LeituraSensor.class))).thenAnswer(i -> i.getArgument(0));

        LeituraResponse resposta = service.registrarLeitura(10L, new LeituraRequest(300.0));

        assertTrue(resposta.alertaGerado());
        assertEquals(NivelRisco.BAIXO, area.getNivelRisco());
    }

    @Test
    void sensorInexistenteLancaExcecao() {
        when(sensorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.registrarLeitura(99L, new LeituraRequest(10.0)));
    }

    @Test
    void calculaNivelPelaRazaoEntreValorELimite() {
        assertEquals(NivelRisco.BAIXO, SensorService.calcularNivel(310, 300));
        assertEquals(NivelRisco.MEDIO, SensorService.calcularNivel(400, 300));
        assertEquals(NivelRisco.ALTO, SensorService.calcularNivel(480, 300));
        assertEquals(NivelRisco.ALTO, SensorService.calcularNivel(10, 0));
    }
}
