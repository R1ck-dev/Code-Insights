package com.projeto.codeinsights.infrastructure.persistence.knowledge.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.ContagemMetrica;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResultadoMetricaJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper.ResultadoMetricaMapper;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.repository.SpringDataResultadoMetricaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResultadoMetricaRepositoryAdapter implements ResultadoMetricaRepository {

    private final SpringDataResultadoMetricaRepository springDataResultadoMetricaRepository;
    private final ResultadoMetricaMapper resultadoMetricaMapper;

    @Override
    public void salvarTodos(List<ResultadoMetrica> resultados) {
        List<ResultadoMetricaJpaEntity> entidades = resultados.stream()
                .map(resultadoMetricaMapper::toEntity)
                .toList();
        springDataResultadoMetricaRepository.saveAll(entidades);
    }

    @Override
    public List<ResultadoMetrica> listarPorResolucao(UUID resolucaoId) {
        return springDataResultadoMetricaRepository.findByResolucaoId(resolucaoId).stream()
                .map(resultadoMetricaMapper::toDomain)
                .toList();
    }

    @Override
    public List<ResultadoMetrica> listarPorResolucoesETipo(List<UUID> resolucaoIds, TipoMetrica tipo) {
        if (resolucaoIds == null || resolucaoIds.isEmpty()) {
            return List.of();
        }
        return springDataResultadoMetricaRepository.findByResolucaoIdInAndTipo(resolucaoIds, tipo).stream()
                .map(resultadoMetricaMapper::toDomain)
                .toList();
    }

    @Override
    public List<ContagemMetrica> contarPorRotulo(UUID autorId, TipoMetrica tipo) {
        return springDataResultadoMetricaRepository.contarPorRotulo(autorId, tipo).stream()
                .map(r -> new ContagemMetrica(
                        (String) r[0],
                        ((Number) r[1]).intValue(),
                        ((Number) r[2]).longValue()))
                .toList();
    }

    @Override
    public void removerPorResolucao(UUID resolucaoId) {
        springDataResultadoMetricaRepository.deleteByResolucaoId(resolucaoId);
    }
}
