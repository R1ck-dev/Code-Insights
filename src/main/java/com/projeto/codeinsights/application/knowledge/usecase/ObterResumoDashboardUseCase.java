package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO;
import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO.AtividadeRecenteDTO;
import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO.DistribuicaoItemDTO;
import com.projeto.codeinsights.application.knowledge.dto.ResumoDashboardDTO.EvolucaoMensalDTO;
import com.projeto.codeinsights.domain.knowledge.enums.GranularidadeTempo;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObterResumoDashboardUseCase {

    private final DesafioRepository desafioRepository;
    private final ResolucaoRepository resolucaoRepository;
    private final SnippetRepository snippetRepository;
    private final ResultadoMetricaRepository resultadoMetricaRepository;

    /** Quantidade de resolucoes mostradas na lista "Atividade recente" do dashboard. */
    private static final int LIMITE_ATIVIDADE_RECENTE = 6;

    @Transactional(readOnly = true)
    public ResumoDashboardDTO execute(UUID autorId) {
        long totalDesafios = desafioRepository.contarPorAutor(autorId);
        long desafiosPublicos = desafioRepository.contarPublicosPorAutor(autorId);
        long totalResolucoes = resolucaoRepository.contarPorAutor(autorId);
        long resolucoesAnalisadas = resolucaoRepository.contarAnalisadasPorAutor(autorId);
        long totalSnippets = snippetRepository.contarPorAutor(autorId);
        long totalCategorias = snippetRepository.contarCategoriasPorAutor(autorId);
        Double mediaAutonomia = resolucaoRepository.mediaAutonomiaPorAutor(autorId);

        List<DistribuicaoItemDTO> bigO = distribuir(autorId, TipoMetrica.BIG_O_TEMPO);
        List<DistribuicaoItemDTO> espaco = distribuir(autorId, TipoMetrica.COMPLEXIDADE_ESPACO);

        List<EvolucaoMensalDTO> evolucao = resolucaoRepository
                .evolucaoPorAutor(autorId, GranularidadeTempo.MENSAL).stream()
                .map(p -> new EvolucaoMensalDTO(p.ano(), p.mes(), p.dia(), p.mediaAutonomia(),
                        p.totalResolucoes(), p.mediaComplexidade()))
                .toList();

        List<AtividadeRecenteDTO> atividadeRecente = resolucaoRepository
                .listarAtividadeRecentePorAutor(autorId, LIMITE_ATIVIDADE_RECENTE).stream()
                .map(a -> new AtividadeRecenteDTO(a.resolucaoId(), a.desafioId(), a.desafioTitulo(),
                        a.linguagem(), a.indiceAutonomiaIA(), a.analisada(),
                        a.complexidadeRotulo(), a.complexidadeOrdem(), a.submetidaEm()))
                .toList();

        return new ResumoDashboardDTO(totalDesafios, desafiosPublicos, totalResolucoes, resolucoesAnalisadas,
                totalSnippets, totalCategorias, mediaAutonomia, bigO, espaco, evolucao, atividadeRecente);
    }

    private List<DistribuicaoItemDTO> distribuir(UUID autorId, TipoMetrica tipo) {
        return resultadoMetricaRepository.contarPorRotulo(autorId, tipo).stream()
                .map(c -> new DistribuicaoItemDTO(c.rotulo(), c.ordem(), c.total()))
                .toList();
    }
}
