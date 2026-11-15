package br.com.fiap.pluvia_api.service;

import br.com.fiap.pluvia_api.dto.LoginRequest;
import br.com.fiap.pluvia_api.dto.RegistroRequest;
import br.com.fiap.pluvia_api.dto.TokenResponse;
import br.com.fiap.pluvia_api.dto.UsuarioResponse;
import br.com.fiap.pluvia_api.exception.ConflitoException;
import br.com.fiap.pluvia_api.model.Perfil;
import br.com.fiap.pluvia_api.model.Usuario;
import br.com.fiap.pluvia_api.repository.UsuarioRepository;
import br.com.fiap.pluvia_api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService service;

    @Test
    void registroGuardaHashDaSenhaECriaCidadao() {
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse resposta = service.registrar(new RegistroRequest("Ana", "  Ana@Teste.com ", "senha1234"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertEquals("HASH", salvo.getSenhaHash());
        assertEquals(Perfil.CIDADAO, salvo.getPerfil());
        assertEquals("ana@teste.com", salvo.getEmail());
        assertEquals(Perfil.CIDADAO, resposta.perfil());
    }

    @Test
    void registroComEmailExistenteLancaConflito() {
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        assertThrows(ConflitoException.class,
                () -> service.registrar(new RegistroRequest("Ana", "ana@teste.com", "senha1234")));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void loginComCredenciaisCorretasDevolveToken() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha1234", "HASH")).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("token-jwt");

        TokenResponse resposta = service.login(new LoginRequest("ana@teste.com", "senha1234"));

        assertEquals("token-jwt", resposta.token());
        assertEquals(Perfil.CIDADAO, resposta.perfil());
    }

    @Test
    void loginComSenhaErradaLancaBadCredentials() {
        when(usuarioRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("errada", "HASH")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("ana@teste.com", "errada")));
    }

    @Test
    void loginComEmailInexistenteLancaBadCredentials() {
        when(usuarioRepository.findByEmail("x@teste.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("x@teste.com", "qualquer")));
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNome("Ana");
        u.setEmail("ana@teste.com");
        u.setSenhaHash("HASH");
        u.setPerfil(Perfil.CIDADAO);
        return u;
    }
}
