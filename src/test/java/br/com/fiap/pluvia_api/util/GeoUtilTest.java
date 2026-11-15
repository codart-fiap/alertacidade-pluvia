package br.com.fiap.pluvia_api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoUtilTest {

    @Test
    void pontosIguaisTemDistanciaZero() {
        assertEquals(0.0, GeoUtil.distanciaMetros(-23.5, -46.6, -23.5, -46.6), 0.001);
    }

    @Test
    void umGrauDeLongitudeNoEquadorTemCercaDe111Km() {
        double distancia = GeoUtil.distanciaMetros(0, 0, 0, 1);
        assertEquals(111_195, distancia, 200);
    }

    @Test
    void distanciaESimetrica() {
        double ida = GeoUtil.distanciaMetros(-23.5160, -46.6280, -23.5050, -46.6000);
        double volta = GeoUtil.distanciaMetros(-23.5050, -46.6000, -23.5160, -46.6280);
        assertEquals(ida, volta, 0.001);
    }
}
