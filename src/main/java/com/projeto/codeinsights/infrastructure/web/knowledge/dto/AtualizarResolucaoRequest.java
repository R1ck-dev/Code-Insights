package com.projeto.codeinsights.infrastructure.web.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

public record AtualizarResolucaoRequest(
        String codigoFonte,
        LinguagemProgramacao linguagem) {
}
