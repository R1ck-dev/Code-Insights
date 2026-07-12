package com.projeto.codeinsights.application.knowledge.dto;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * O "antes -> depois" de uma metrica reprocessada: e a evidencia de que a evolucao do motor
 * mudou de fato a classificacao do corpus (sem isso, reanalisar seria um no-op cego).
 * <p>
 * Campos {@code *Antes} nulos = a metrica nao existia (resolucao nunca analisada, ou tipo de
 * metrica criado depois da submissao). Mudanca so de confianca conta: e exatamente o caso das
 * linhas anteriores a V7, que herdaram {@code BAIXA} do DEFAULT da coluna mesmo quando o valor
 * era exato (ciclomatica), o que faz a plataforma afirmar algo falso sobre o proprio dado.
 */
public record MudancaMetricaDTO(
        TipoMetrica tipo,
        String rotuloAntes,
        String rotuloDepois,
        NivelConfianca confiancaAntes,
        NivelConfianca confiancaDepois) {
}
