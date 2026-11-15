package br.com.fiap.pluvia_api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso:
 *  - publico: /auth/** (registro e login) e a documentacao Swagger;
 *  - DISPOSITIVO (sensor com X-Device-Key) e ADMIN: enviar leituras (POST /sensores/{id}/leituras);
 *  - leituras (GET) dos recursos de consulta: públicas para atender o mapa cidadão;
 *  - ADMIN: qualquer outra escrita (POST, PUT, DELETE).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Device-Key"));
        configuration.setExposedHeaders(java.util.List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                           @Value("${app.device-key}") String chaveDispositivo) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable()) // API stateless com token no cabecalho, sem cookies de sessao
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/error",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/sensores/*/leituras")
                                .hasAnyRole("ADMIN", "DISPOSITIVO")
                        .requestMatchers(HttpMethod.GET,
                                "/areas-risco", "/areas-risco/**",
                                "/sensores", "/sensores/**",
                                "/alertas", "/alertas/**",
                                "/pontos-seguros", "/pontos-seguros/**",
                                "/recomendacao", "/recomendacao/**").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escreverErro(res, 401, "Autenticação necessária"))
                        .accessDeniedHandler((req, res, ex) -> escreverErro(res, 403, "Acesso negado para o seu perfil")))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new DeviceKeyFilter(chaveDispositivo), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void escreverErro(jakarta.servlet.http.HttpServletResponse res, int status, String mensagem)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"mensagem\":\"" + mensagem + "\"}");
    }
}
