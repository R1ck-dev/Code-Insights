package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.MudancaMetricaDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO.Status;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Reprocessa as metricas de UMA resolucao ja gravada: roda o motor ATUAL sobre o codigo-fonte que
 * esta no banco, substitui os resultados antigos e devolve o diff (antes -> depois).
 * <p>
 * Recalcula <b>somente o que o motor deriva do codigo</b> (Big O de tempo, complexidade de espaco,
 * ciclomatica). O Indice de Autonomia IA e a descricao de apoio sao AUTODECLARADOS pelo aluno — sao
 * dado primario da pesquisa, nao pertencem ao motor — e a data de submissao e o eixo do tempo do
 * estudo: nenhum dos tres, nem a visibilidade, e tocado aqui.
 * <p>
 * Difere do {@link AnalisarResolucaoUseCase} (que roda na submissao) em dois pontos que so importam
 * quando ja existe dado gravado:
 * <ul>
 *   <li><b>Analisa antes de apagar</b>: os resultados antigos so caem se o motor produzir novos.
 *       Codigo que nao parseia, ou linguagem sem analisador, preserva o que ja havia — reprocessar
 *       nunca destroi dado de pesquisa nem marca como analisada uma resolucao que nao foi.</li>
 *   <li><b>Reporta o que mudou</b>, incluindo mudanca so de confianca.</li>
 * </ul>
 * Transacional por resolucao: uma linha problematica falha sozinha, sem abortar a passada inteira do
 * corpus (ver {@link ReanalisarResolucoesUseCase}).
 */
@Service
@RequiredArgsConstructor
public class ReanalisarResolucaoUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final ResultadoMetricaRepository resultadoMetricaRepository;
    private final AnalisadorMetricas analisadorMetricas;

    @Transactional
    public ReanaliseResolucaoDTO execute(UUID resolucaoId) {
        Resolucao resolucao = resolucaoRepository.buscarPorId(resolucaoId)
                .orElseThrow(() -> new NegocioException("Resolucao nao encontrada."));

        if (!analisadorMetricas.suporta(resolucao.getLinguagem())) {
            return new ReanaliseResolucaoDTO(resolucaoId, Status.PULADA, List.of());
        }

        List<ResultadoMetrica> novos = analisadorMetricas.analisar(resolucao);
        if (novos.isEmpty()) {
            return new ReanaliseResolucaoDTO(resolucaoId, Status.FALHOU, List.of());
        }

        List<MudancaMetricaDTO> mudancas = compararComOGravado(resolucaoId, novos);

        resultadoMetricaRepository.removerPorResolucao(resolucaoId);
        resultadoMetricaRepository.salvarTodos(novos);

        resolucao.marcarComoAnalisada();
        resolucaoRepository.salvar(resolucao);

        return new ReanaliseResolucaoDTO(resolucaoId, Status.REPROCESSADA, mudancas);
    }

    /** So o que de fato mudou de rotulo ou de confianca; metrica inedita entra com o "antes" nulo. */
    private List<MudancaMetricaDTO> compararComOGravado(UUID resolucaoId, List<ResultadoMetrica> novos) {
        Map<TipoMetrica, ResultadoMetrica> gravados = new EnumMap<>(TipoMetrica.class);
        resultadoMetricaRepository.listarPorResolucao(resolucaoId)
                .forEach(gravado -> gravados.put(gravado.getTipo(), gravado));

        List<MudancaMetricaDTO> mudancas = new ArrayList<>();
        for (ResultadoMetrica novo : novos) {
            ResultadoMetrica gravado = gravados.get(novo.getTipo());
            String rotuloAntes = (gravado != null) ? gravado.getRotulo() : null;
            NivelConfianca confiancaAntes = (gravado != null) ? gravado.getConfianca() : null;

            if (Objects.equals(rotuloAntes, novo.getRotulo()) && confiancaAntes == novo.getConfianca()) {
                continue;
            }
            mudancas.add(new MudancaMetricaDTO(novo.getTipo(), rotuloAntes, novo.getRotulo(),
                    confiancaAntes, novo.getConfianca()));
        }
        return mudancas;
    }
}
