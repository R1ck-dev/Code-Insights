package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

public record ResultadoMetricaDTO(
        TipoMetrica tipo,
        int valor,
        String rotulo,
        String detalhe,
        NivelConfianca confianca,
        OffsetDateTime analisadoEm) {
}
