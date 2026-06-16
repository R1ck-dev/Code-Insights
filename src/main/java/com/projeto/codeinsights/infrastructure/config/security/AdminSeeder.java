package com.projeto.codeinsights.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.PasswordEncoderPort;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Value("${admin.default.email}")
    private String adminEmail;

    @Value("${admin.default.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        log.info("Verificando conta administradora padrao...");

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD nao definido: a conta administradora padrao NAO sera criada. "
                    + "Defina ADMIN_PASSWORD para semear um admin.");
            return;
        }

        if (!usuarioRepository.existePorEmail(adminEmail)) {
            log.info("Conta admin '{}' nao encontrada. Criando...", adminEmail);

            String hash = passwordEncoder.encode(adminPassword);

            Usuario admin = new Usuario(null, "admin", adminEmail, hash);
            admin.promoverParaAdmin();
            admin.ativarConta();

            usuarioRepository.salvar(admin);

            log.info("Conta administradora padrao criada com sucesso.");
        } else {
            log.info("Conta administradora padrao ja existe. Nenhuma acao necessaria.");
        }
    }
}
