package br.com.fiap.pluvia_api.security;

import br.com.fiap.pluvia_api.model.Perfil;
import br.com.fiap.pluvia_api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";

    private Usuario admin() {
        Usuario u = new Usuario();
        u.setEmail("admin@teste.com");
        u.setPerfil(Perfil.ADMIN);
        return u;
    }

    @Test
    void tokenGeradoCarregaEmailEPerfil() {
        JwtService service = new JwtService(SEGREDO, 5);

        Claims claims = service.lerClaims(service.gerarToken(admin()));

        assertEquals("admin@teste.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("perfil", String.class));
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        JwtService service = new JwtService(SEGREDO, 5);
        String token = service.gerarToken(admin());
        String adulterado = token.substring(0, token.length() - 3) + "abc";

        assertThrows(JwtException.class, () -> service.lerClaims(adulterado));
    }

    @Test
    void tokenAssinadoComOutraChaveEhRejeitado() {
        JwtService emissor = new JwtService("outra-chave-totalmente-diferente-com-32-chars!!", 5);
        JwtService validador = new JwtService(SEGREDO, 5);

        assertThrows(JwtException.class, () -> validador.lerClaims(emissor.gerarToken(admin())));
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        JwtService service = new JwtService(SEGREDO, -1);

        assertThrows(JwtException.class, () -> service.lerClaims(service.gerarToken(admin())));
    }

    @Test
    void segredoCurtoEhRecusado() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto", 5));
    }
}
