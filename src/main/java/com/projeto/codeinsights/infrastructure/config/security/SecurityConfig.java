package com.projeto.codeinsights.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // --- Rotas publicas (acesso e recuperacao de conta) ---
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/reenviar-ativacao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/esqueci-senha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/redefinir-senha").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/ativar").permitAll()
                        // Perfil proprio e diretorio de portfolios exigem autenticacao
                        // (precedem a regra publica /api/usuarios/* logo abaixo).
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/publicos").authenticated()
                        // Portfolio publico de um autor
                        .requestMatchers(HttpMethod.GET, "/api/desafios/autor/**").permitAll()
                        // Leitura anonima de recursos publicos (a visibilidade e validada no use case;
                        // recurso privado -> NegocioException 400). '*' casa exatamente um segmento,
                        // entao GET /api/desafios (lista do dono) e as escritas (POST/PATCH/DELETE) seguem protegidas.
                        .requestMatchers(HttpMethod.GET, "/api/desafios/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/desafios/*/resolucoes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resolucoes/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resolucoes/*/metricas").permitAll()
                        // Perfil publico de um usuario por id
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*").permitAll()
                        // Documentacao
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Area administrativa
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Demais rotas exigem autenticacao
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
