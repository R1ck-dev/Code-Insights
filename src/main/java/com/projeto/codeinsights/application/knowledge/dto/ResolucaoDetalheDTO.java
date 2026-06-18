package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

public record ResolucaoDetalheDTO(
        UUID id,
        UUID desafioId,
        UUID autorId,
        String codigoFonte,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        String descricaoApoioIA,
        Visibilidade visibilidade,
        boolean analisada,
        OffsetDateTime submetidaEm) {
}
