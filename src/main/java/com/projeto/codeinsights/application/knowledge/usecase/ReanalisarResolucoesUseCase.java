package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO.Status;
import com.projeto.codeinsights.application.knowledge.dto.RelatorioReanaliseDTO;
import com.projeto.codeinsights.application.knowledge.dto.RelatorioReanaliseDTO.ResolucaoAlteradaDTO;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reanalisa as resolucoes ja gravadas com o motor ATUAL de metricas e devolve o relatorio da passada.
 * <p>
 * Existe porque o motor evolui (custo simbolico interprocedural, Teorema Mestre, tabela de custo do
 * JDK, nivel de confianca...) e, quando ele evolui, as resolucoes analisadas por versoes anteriores
 * deixam de ser comparaveis com as novas — o que quebraria a serie historica que a pesquisa mede.
 * Reprocessar o corpus e, portanto, infraestrutura recorrente do estudo, e nao um script de uma vez so.
 * <p>
 * Idempotente: com o mesmo motor, a segunda passada regrava os mesmos valores e nao reporta mudanca
 * alguma. <b>Nao</b> e transacional de proposito — cada resolucao e reprocessada na sua propria
 * transacao ({@link ReanalisarResolucaoUseCase}), entao uma linha problematica falha sozinha, entra
 * no relatorio como falha, e o resto do corpus segue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReanalisarResolucoesUseCase {

    private final ResolucaoRepository resolucaoRepository;
    private final ReanalisarResolucaoUseCase reanalisarResolucaoUseCase;

    /** @param autorId restringe a passada as resolucoes de um autor; {@code null} = corpus inteiro. */
    public RelatorioReanaliseDTO execute(UUID autorId) {
        List<UUID> ids = (autorId != null)
                ? resolucaoRepository.listarIdsPorAutor(autorId)
                : resolucaoRepository.listarTodosIds();

        log.info("Reanalise de metricas iniciada ({}): {} resolucao(oes) na fila.",
                (autorId != null) ? "autor " + autorId : "corpus inteiro", ids.size());

        int reprocessadas = 0;
        int puladas = 0;
        int falhas = 0;
        List<ResolucaoAlteradaDTO> alteracoes = new ArrayList<>();

        for (UUID id : ids) {
            ReanaliseResolucaoDTO resultado = reanalisarIsolando(id);
            switch (resultado.status()) {
                case REPROCESSADA -> {
                    reprocessadas++;
                    if (!resultado.mudancas().isEmpty()) {
                        alteracoes.add(new ResolucaoAlteradaDTO(id, resultado.mudancas()));
                    }
                }
                case PULADA -> puladas++;
                case FALHOU -> falhas++;
            }
        }

        log.info("Reanalise concluida: {} reprocessada(s), {} pulada(s), {} falha(s), {} com mudanca.",
                reprocessadas, puladas, falhas, alteracoes.size());

        return new RelatorioReanaliseDTO(ids.size(), reprocessadas, puladas, falhas,
                alteracoes.size(), List.copyOf(alteracoes));
    }

    /** Uma resolucao ruim (codigo patologico, motor quebrando) nao pode derrubar a passada inteira. */
    private ReanaliseResolucaoDTO reanalisarIsolando(UUID id) {
        try {
            return reanalisarResolucaoUseCase.execute(id);
        } catch (RuntimeException ex) {
            log.warn("Falha ao reanalisar a resolucao {}: {}", id, ex.toString());
            return new ReanaliseResolucaoDTO(id, Status.FALHOU, List.of());
        }
    }
}
