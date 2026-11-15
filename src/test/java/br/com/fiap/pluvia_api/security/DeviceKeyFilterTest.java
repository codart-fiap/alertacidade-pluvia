package br.com.fiap.pluvia_api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceKeyFilterTest {

    private static final String CHAVE = "chave-de-teste-do-dispositivo";

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private Authentication executar(String cabecalho) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sensores/1/leituras");
        if (cabecalho != null) {
            request.addHeader("X-Device-Key", cabecalho);
        }
        new DeviceKeyFilter(CHAVE).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void chaveCorretaAutenticaComoDispositivo() throws Exception {
        Authentication auth = executar(CHAVE);

        assertNotNull(auth);
        assertEquals("ROLE_DISPOSITIVO", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void chaveErradaNaoAutentica() throws Exception {
        assertNull(executar("chave-errada-qualquer"));
    }

    @Test
    void semCabecalhoNaoAutentica() throws Exception {
        assertNull(executar(null));
    }

    @Test
    void chaveConfiguradaCurtaEhRecusada() {
        assertThrows(IllegalStateException.class, () -> new DeviceKeyFilter("curta"));
    }
}
