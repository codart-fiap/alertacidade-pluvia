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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** O registro publico sempre cria um CIDADAO; administradores sao criados fora da API. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        usuario.setPerfil(Perfil.CIDADAO);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!passwordEncoder.matches(req.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return new TokenResponse(jwtService.gerarToken(usuario), "Bearer", usuario.getPerfil());
    }
}
