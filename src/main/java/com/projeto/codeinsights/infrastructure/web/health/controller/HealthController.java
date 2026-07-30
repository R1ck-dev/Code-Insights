package com.projeto.codeinsights.infrastructure.web.health.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Sinal de vida da aplicacao, usado pelo health check da hospedagem e pelo ping
 * externo que impede a hibernacao do servico no tier gratuito.
 * <p>
 * <b>Nao toca o banco de proposito.</b> O Postgres gerenciado (Neon) suspende apos
 * alguns minutos ocioso e o plano gratuito da uma cota mensal de horas de computo;
 * um health check que abrisse conexao — como faz o indicador padrao do Spring Boot
 * Actuator — manteria o banco acordado 24h por dia e queimaria a cota em poucos dias.
 * Por isso aqui e um endpoint proprio e deliberadamente burro, e nao o Actuator:
 * ele responde "o processo Java esta de pe", nada mais.
 * <p>
 * Fica fora de {@code /api} por ser operacional, e nao parte da API do produto: assim
 * nao entra no CORS (mapeado em {@code /api/**}) nem na documentacao OpenAPI.
 */
@RestController
@Hidden
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
