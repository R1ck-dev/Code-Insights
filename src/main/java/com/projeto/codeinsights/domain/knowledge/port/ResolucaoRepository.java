package com.projeto.codeinsights.domain.knowledge.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.model.AtividadeRecente;
import com.projeto.codeinsights.domain.knowledge.model.PontoEvolucaoMensal;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.shared.Pagina;

public interface ResolucaoRepository {
    Resolucao salvar(Resolucao resolucao);

    Optional<Resolucao> buscarPorId(UUID id);

    Pagina<Resolucao> listarPorDesafio(UUID desafioId, int pagina, int tamanho);

    Pagina<Resolucao> listarPublicasPorDesafio(UUID desafioId, int pagina, int tamanho);

    long contarPorDesafio(UUID desafioId);

    long contarPublicasPorDesafio(UUID desafioId);

    long contarPorAutor(UUID autorId);

    /** Quantas resolucoes do autor ja tiveram a analise de metricas concluida. */
    long contarAnalisadasPorAutor(UUID autorId);

    /** Media do Indice de Autonomia IA das resolucoes do autor; null se nao houver resolucoes. */
    Double mediaAutonomiaPorAutor(UUID autorId);

    List<PontoEvolucaoMensal> evolucaoMensalPorAutor(UUID autorId);

    /** As {@code limite} resolucoes mais recentes do autor, com titulo do desafio e Big O de tempo. */
    List<AtividadeRecente> listarAtividadeRecentePorAutor(UUID autorId, int limite);

    void remover(UUID id);
}
