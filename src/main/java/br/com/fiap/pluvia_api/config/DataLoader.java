package br.com.fiap.pluvia_api.config;

import br.com.fiap.pluvia_api.model.AreaRisco;
import br.com.fiap.pluvia_api.model.NivelRisco;
import br.com.fiap.pluvia_api.model.Perfil;
import br.com.fiap.pluvia_api.model.PontoSeguro;
import br.com.fiap.pluvia_api.model.Sensor;
import br.com.fiap.pluvia_api.model.TipoSensor;
import br.com.fiap.pluvia_api.model.Usuario;
import br.com.fiap.pluvia_api.repository.AreaRiscoRepository;
import br.com.fiap.pluvia_api.repository.PontoSeguroRepository;
import br.com.fiap.pluvia_api.repository.SensorRepository;
import br.com.fiap.pluvia_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga inicial de dados ficticios. A conta ADMIN só é criada quando credenciais locais
 * explícitas são fornecidas; não há senha administrativa padrão no código.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final AreaRiscoRepository areaRepository;
    private final SensorRepository sensorRepository;
    private final PontoSeguroRepository pontoRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataLoader(UsuarioRepository usuarioRepository, AreaRiscoRepository areaRepository,
                      SensorRepository sensorRepository, PontoSeguroRepository pontoRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.bootstrap.admin-email:}") String adminEmail,
                      @Value("${app.bootstrap.admin-password:}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
        this.sensorRepository = sensorRepository;
        this.pontoRepository = pontoRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() != adminPassword.isBlank()) {
            throw new IllegalStateException("Defina ADMIN_EMAIL e ADMIN_PASSWORD juntos para criar a conta ADMIN inicial.");
        }
        if (!adminEmail.isBlank()) {
            String email = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
            if (!usuarioRepository.existsByEmail(email)) {
                usuarioRepository.save(usuario("Administrador", email, adminPassword, Perfil.ADMIN));
            }
        }
        if (areaRepository.count() == 0) {
            AreaRisco areaA = areaRepository.save(
                    area("Área A - Várzea do rio (demo)", -23.5160, -46.6280, 700, NivelRisco.ALTO, true));
            AreaRisco areaB = areaRepository.save(
                    area("Área B - Córrego (demo)", -23.5270, -46.6870, 500, NivelRisco.MEDIO, false));

            sensorRepository.save(sensor("Régua de nível - Área A", TipoSensor.NIVEL_RIO, areaA, 300));
            sensorRepository.save(sensor("Pluviômetro - Área A", TipoSensor.CHUVA, areaA, 50));
            sensorRepository.save(sensor("Régua de nível - Área B", TipoSensor.NIVEL_RIO, areaB, 150));
            sensorRepository.save(sensor("Pluviômetro - Área B", TipoSensor.CHUVA, areaB, 40));

            pontoRepository.save(ponto("Escola Municipal - ponto de apoio (demo)", -23.5050, -46.6000));
            pontoRepository.save(ponto("Quadra Poliesportiva - ponto de apoio (demo)", -23.5300, -46.6150));
            pontoRepository.save(ponto("Centro Comunitário - ponto de apoio (demo)", -23.5400, -46.6800));
        }
    }

    private Usuario usuario(String nome, String email, String senha, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setPerfil(perfil);
        return u;
    }

    private AreaRisco area(String nome, double lat, double lng, double raio, NivelRisco nivel, boolean ativa) {
        AreaRisco a = new AreaRisco();
        a.setNome(nome);
        a.setLatitude(lat);
        a.setLongitude(lng);
        a.setRaioMetros(raio);
        a.setNivelRisco(nivel);
        a.setAtiva(ativa);
        return a;
    }

    private Sensor sensor(String nome, TipoSensor tipo, AreaRisco area, double limite) {
        Sensor s = new Sensor();
        s.setNome(nome);
        s.setTipo(tipo);
        s.setAreaRisco(area);
        s.setLimiteAlerta(limite);
        return s;
    }

    private PontoSeguro ponto(String nome, double lat, double lng) {
        PontoSeguro p = new PontoSeguro();
        p.setNome(nome);
        p.setLatitude(lat);
        p.setLongitude(lng);
        return p;
    }
}
