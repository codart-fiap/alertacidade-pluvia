package br.com.fiap.pluvia_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica os DISPOSITIVOS (ESP32 do Wokwi, simulador) pelo cabecalho "X-Device-Key".
 * Se a chave confere, a requisicao ganha o perfil DISPOSITIVO, que so pode enviar leituras
 * (ver SecurityConfig). Assim o sensor nao precisa guardar login nem token que expira.
 * Chave ausente ou errada: segue sem autenticacao e as regras do SecurityConfig respondem 401.
 */
public class DeviceKeyFilter extends OncePerRequestFilter {

    static final String CABECALHO = "X-Device-Key";

    private final byte[] chaveEsperada;

    public DeviceKeyFilter(String chave) {
        if (chave == null || chave.length() < 16) {
            throw new IllegalStateException("app.device-key precisa ter pelo menos 16 caracteres");
        }
        this.chaveEsperada = chave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recebida = request.getHeader(CABECALHO);
        if (recebida != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // MessageDigest.isEqual compara em tempo constante (evita ataque de timing)
            if (MessageDigest.isEqual(chaveEsperada, recebida.getBytes(StandardCharsets.UTF_8))) {
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        "dispositivo", null, List.of(new SimpleGrantedAuthority("ROLE_DISPOSITIVO")));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        }
        chain.doFilter(request, response);
    }
}
