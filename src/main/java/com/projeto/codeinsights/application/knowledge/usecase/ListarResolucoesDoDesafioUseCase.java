package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResolucaoResumoDTO;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Lista as resolucoes de um desafio ja com a complexidade de TEMPO de cada uma:
 * e nesta tela que o aluno compara suas tentativas lado a lado, e a trajetoria de
 * amadurecimento algoritmico e o dado central da pesquisa.
 * <p>
 * <b>Sem N+1</b>: sao exatamente DUAS consultas, independentemente do tamanho da
 * pagina — a pagina de resolucoes e um unico lote com as metricas de
 * {@code BIG_O_TEMPO} das resolucoes dessa pagina, casado em memoria por id.
 */
@Service
@RequiredArgsConstructor
public class ListarResolucoesDoDesafioUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final DesafioRepository desafioRepository;
    private final ResultadoMetricaRepository resultadoMetricaRepository;

    @Transactional(readOnly = true)
    public Pagina<ResolucaoResumoDTO> execute(UUID desafioId, UUID solicitanteId, int pagina, int tamanho) {
        Desafio desafio = desafioRepository.buscarPorId(desafioId)
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        boolean dono = desafio.pertenceA(solicitanteId);
        if (!dono && !desafio.ehPublico()) {
            throw new NegocioException("Voce nao tem permissao para ver as resolucoes deste desafio.");
        }

        // Dono ve todas as suas resolucoes; visitante/nao-dono ve apenas as publicas.
        Pagina<Resolucao> resolucoes = dono
                ? resolucaoRepository.listarPorDesafio(desafioId, pagina, tamanho)
                : resolucaoRepository.listarPublicasPorDesafio(desafioId, pagina, tamanho);

        Map<UUID, ResultadoMetrica> tempoPorResolucao = buscarTempoEmLote(resolucoes.itens());

        return new Pagina<>(
                resolucoes.itens().stream()
                        .map(r -> toResumo(r, tempoPorResolucao.get(r.getId())))
                        .toList(),
                resolucoes.paginaAtual(),
                resolucoes.totalPaginas(),
                resolucoes.totalItens());
    }

    /**
     * Uma unica consulta para toda a pagina. Resolucao sem entrada no mapa = sem dado
     * (nao analisada, ou linguagem sem analisador) — e isso vira {@code null} no DTO,
     * jamais um zero, que seria {@code O(1)}.
     */
    private Map<UUID, ResultadoMetrica> buscarTempoEmLote(List<Resolucao> resolucoes) {
        List<UUID> ids = resolucoes.stream().map(Resolucao::getId).toList();
        return resultadoMetricaRepository.listarPorResolucoesETipo(ids, TipoMetrica.BIG_O_TEMPO).stream()
                .collect(Collectors.toMap(ResultadoMetrica::getResolucaoId, Function.identity(),
                        (primeiro, segundo) -> primeiro));
    }

    /** {@code tempo == null} significa ausencia de dado; {@code valor == -1}, DESCONHECIDO. Sao distintos. */
    private ResolucaoResumoDTO toResumo(Resolucao resolucao, ResultadoMetrica tempo) {
        String tempoRotulo = (tempo != null) ? tempo.getRotulo() : null;
        Integer tempoOrdem = (tempo != null) ? Integer.valueOf(tempo.getValor()) : null;
        NivelConfianca confiancaTempo = (tempo != null) ? tempo.getConfianca() : null;

        return new ResolucaoResumoDTO(
                resolucao.getId(),
                resolucao.getDesafioId(),
                resolucao.getAutorId(),
                resolucao.getLinguagem(),
                resolucao.getIndiceAutonomiaIA(),
                resolucao.getVisibilidade(),
                resolucao.isAnalisada(),
                tempoRotulo,
                tempoOrdem,
                confiancaTempo,
                resolucao.getSubmetidaEm());
    }
}
