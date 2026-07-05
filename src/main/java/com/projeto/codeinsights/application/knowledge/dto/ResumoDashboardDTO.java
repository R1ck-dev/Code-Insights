package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Agregados do dashboard do aluno logado. {@code mediaAutonomia} e {@code null}
 * quando o usuario ainda nao tem resolucoes.
 */
public record ResumoDashboardDTO(
        long totalDesafios,
        long desafiosPublicos,
        long totalResolucoes,
        long resolucoesAnalisadas,
        long totalSnippets,
        long totalCategorias,
        Double mediaAutonomia,
        List<DistribuicaoItemDTO> distribuicaoBigO,
        List<DistribuicaoItemDTO> distribuicaoEspaco,
        List<EvolucaoMensalDTO> evolucao,
        List<AtividadeRecenteDTO> atividadeRecente) {

    /** ordem = ordinal da classe de complexidade (ordena a curva); rotulo = ex. "O(n^2)". */
    public record DistribuicaoItemDTO(String rotulo, int ordem, long total) {
    }

    /** {@code mediaComplexidade} = media da ordem de Big O (tempo) das resolucoes do mes; null se nenhuma analisada. */
    public record EvolucaoMensalDTO(int ano, int mes, Double mediaAutonomia, long totalResolucoes,
            Double mediaComplexidade) {
    }

    /**
     * Uma entrada da lista "Atividade recente". {@code complexidadeRotulo}/{@code complexidadeOrdem}
     * sao null quando a resolucao ainda nao foi analisada ou nao gerou metrica de tempo.
     */
    public record AtividadeRecenteDTO(UUID resolucaoId, UUID desafioId, String desafioTitulo,
            LinguagemProgramacao linguagem, int indiceAutonomiaIA, boolean analisada,
            String complexidadeRotulo, Integer complexidadeOrdem, OffsetDateTime submetidaEm) {
    }
}
