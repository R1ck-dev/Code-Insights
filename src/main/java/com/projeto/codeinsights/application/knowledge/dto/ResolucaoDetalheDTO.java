package com.projeto.codeinsights.application.knowledge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResolucaoDetalheDTO(
        UUID id,
        UUID desafioId,
        UUID autorId,
        String linguagem,
        String codigoFonte,
        Integer indiceAutonomiaIa,
        String complexidadeTempo,
        String complexidadeEspaco,
        Integer complexidadeCiclomatica,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao) {
}
